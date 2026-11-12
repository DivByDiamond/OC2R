package li.cil.oc2.common.inet.layer.impl;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.api.inet.*;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.DatagramSession;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.inet.session.echo.EchoHandler;
import li.cil.oc2.common.inet.session.echo.EchoResponse;
import li.cil.oc2.common.inet.session.manager.SocketManager;
import li.cil.oc2.common.inet.session.manager.ready.ReadySessions;
import li.cil.oc2.common.inet.session.manager.ready.SessionChannelHelper;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Session layer bridging OC sessions to real JDK socket channels ({@link SocketChannel} and
 * {@link DatagramChannel}) managed by a {@link SocketManager}.
 *
 * <p>All methods are invoked from the internet thread while the TCP/IP stack processes frames.
 * The layer is pull-based: {@link #receiveSession(Receiver)} is asked for one ready session at a
 * time and must hand over at most one piece of work per call, so readiness queues filled by the
 * socket selector are drained step by step.
 */
public final class DefaultSessionLayer implements SessionLayer {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Queue<EchoResponse> echoResponse = EchoHandler.newResponseQueue();

    // Largest UDP payload; only the internet thread reads datagrams, so one buffer per layer is enough.
    private static final int MAX_DATAGRAM_SIZE = 65535;
    private static final int DNS_PORT = 53;
    private static final int DNS_HEADER_SIZE = 12;
    private static final int DNS_FLAGS_OFFSET = 2;
    private static final byte DNS_TRUNCATED = 0x02;

    private final ByteBuffer datagramScratch = ByteBuffer.allocate(MAX_DATAGRAM_SIZE);
    private final ReadySessions readySessions = new ReadySessions();

    /**
     * Every stream session this layer opened a channel for. Used to opportunistically
     * push buffered data towards the VM even when no socket is currently readable;
     * entries whose session left the ESTABLISHED state are pruned lazily.
     */
    private final Set<StreamSessionImpl> trackedStreams = ConcurrentHashMap.newKeySet();
    private final SocketManager socketManager;

    public DefaultSessionLayer(final LayerParameters layerParameters) {
        InternetManager internetManager = layerParameters.getInternetManager();
        socketManager = SocketManager.attach(internetManager);
    }

    @Override
    public void onStop() {
        socketManager.detach();
    }

    @Override
    public void receiveSession(final Receiver receiver) {
        if (EchoHandler.deliverPendingResponse(echoResponse, receiver)) {
            return;
        }

        final boolean somethingConnected =
                SessionChannelHelper.processQueue(
                        readySessions.getToConnect(),
                        session -> connectStream(receiver, session));
        if (somethingConnected) {
            return;
        }

        if (SessionChannelHelper.processQueue(
                readySessions.getToRead(), session -> readSession(receiver, session))) {
            return;
        }

        // No I/O-ready sessions: opportunistically keep draining unsent receive-buffer
        // data of established streams. Without this, exactly one ~MTU segment left per
        // game tick (readiness is only enqueued once per tick), capping throughput at
        // ~30 KB/s regardless of the sliding window size.
        pollUnsentStream(receiver);
    }

    /**
     * Completes the pending non-blocking {@code connect()} of a stream session. The session is
     * first handed to the receiver (which registers it with the transport layer), then the
     * connection attempt is finished and the session enters its handshake state; on failure the
     * session is closed, which makes the transport layer emit a RST towards the VM.
     */
    private boolean connectStream(final Receiver receiver, final Session session) {
        if (!(session instanceof StreamSession streamSession)) {
            return false;
        }
        LOGGER.trace("Connected {}", session);
        if (session.getState() != Session.States.NEW) {
            return false;
        }
        receiver.receive(streamSession);
        try {
            final SocketChannel channel = SessionChannelHelper.getChannel(streamSession);
            channel.finishConnect();
            streamSession.connect();
            return true;
        } catch (final ConnectException exception) {
            LOGGER.trace("Connection rejected for {}", session);
            SessionChannelHelper.closeSession(session);
            return true;
        } catch (final IOException exception) {
            LOGGER.error("Error on socket.finishConnect()", exception);
            SessionChannelHelper.closeSession(session);
            return true;
        }
    }

    private boolean readSession(final Receiver receiver, final Session session) {
        if (session instanceof DatagramSession datagramSession) {
            return readDatagram(receiver, datagramSession);
        }
        // Stream sessions are the only other session kind; anything else has no socket.
        return session instanceof StreamSession streamSession
                && readStream(receiver, streamSession);
    }

    private boolean readDatagram(
            final Receiver receiver, final DatagramSession datagramSession) {
        LOGGER.trace("Datagram received");
        final DatagramChannel channel = SessionChannelHelper.getChannel(datagramSession);
        try {
            // Read into a buffer of the largest UDP size first. The room in a frame is about 1470 bytes and the
            // channel silently discards what does not fit, which delivered half a DNS answer to the guest.
            datagramScratch.clear();
            final SocketAddress address = channel.receive(datagramScratch);
            if (address == null) {
                return false;
            }
            if (Config.useSynchronisedNAT
                    && !address.equals(datagramSession.getDestination())) {
                // With synchronised NAT the socket is bound to a wildcard address, so drop
                // datagrams that did not come from the session's expected destination.
                return false;
            }
            datagramScratch.flip();
            final ByteBuffer datagram = receiver.receive(datagramSession);
            assert datagram != null;
            if (!placeDatagram(datagram, datagramSession)) {
                receiver.cancel();
                return false;
            }
            datagram.flip();
            return true;
        } catch (final IOException exception) {
            LOGGER.error("Trying to read datagram socket", exception);
        }
        return false;
    }

    /**
     * Copies the received datagram into the frame. One that does not fit cannot be delivered without IP
     * fragmentation. A DNS answer is cut and flagged as truncated, which makes the resolver ask again over TCP;
     * anything else is dropped, since half a datagram is useless to every protocol.
     */
    private boolean placeDatagram(final ByteBuffer frame, final DatagramSession session) {
        final int length = datagramScratch.remaining();
        if (length <= frame.remaining()) {
            frame.put(datagramScratch);
            return true;
        }
        if (session.getDestination().getPort() == DNS_PORT && frame.remaining() >= DNS_HEADER_SIZE) {
            final int room = frame.remaining();
            final int start = frame.position();
            datagramScratch.limit(datagramScratch.position() + room);
            frame.put(datagramScratch);
            frame.put(start + DNS_FLAGS_OFFSET, (byte) (frame.get(start + DNS_FLAGS_OFFSET) | DNS_TRUNCATED));
            LOGGER.trace("DNS answer of {} bytes truncated to {}", length, room);
            return true;
        }
        LOGGER.trace("Dropped a datagram of {} bytes that does not fit into a frame", length);
        return false;
    }

    private boolean readStream(final Receiver receiver, final StreamSession streamSession) {
        LOGGER.trace("Stream received");
        try {
            final SocketChannel channel = SessionChannelHelper.getChannel(streamSession);
            final ByteBuffer stream = receiver.receive(streamSession);
            assert stream != null;
            if (streamSession instanceof StreamSessionImpl pending && pending.isInputEnded()) return true;
            // Non-blocking channel: read until EAGAIN (0), end of stream (-1)
            // or the session buffer is full to make use of the whole window.
            for (;;) {
                final int read = channel.read(stream);
                if (read <= 0) {
                    if (read == -1) {
                        if (streamSession instanceof StreamSessionImpl pending) {
                            // The same read loop may have received the complete response before EOF.
                            // Keep its bytes available until the guest ACKs them, then send FIN.
                            channel.close();
                            pending.endOfInput();
                        } else {
                            SessionChannelHelper.closeSession(streamSession);
                        }
                        break;
                    }
                    break;
                }
                LOGGER.trace("Read from real world: {}", read);
            }
            return true;
        } catch (final IOException exception) {
            LOGGER.error("Trying to read stream socket", exception);
        }
        return false;
    }

    /**
     * Offers the receiver the first established stream that still has unsent buffered
     * data, so the transport layer builds another segment without waiting for inbound
     * traffic. Returns false when every tracked stream is fully drained (or gone).
     */
    private void pollUnsentStream(final Receiver receiver) {
        final Iterator<StreamSessionImpl> iterator = trackedStreams.iterator();
        while (iterator.hasNext()) {
            final StreamSessionImpl stream = iterator.next();
            if (stream.isClosed() || !stream.getState().equals(Session.States.ESTABLISHED)) {
                iterator.remove();
            } else if (stream.receiveBuffer.position() > stream.nextSegmentMark || stream.hasPendingFin()) {
                receiver.receive(stream);
                return;
            }
        }
    }

    @Override
    public void sendSession(final Session session, @Nullable final ByteBuffer data) {
        if (EchoHandler.handleEchoSession(session, data, echoResponse)) {
            return;
        }

        if (session instanceof DatagramSession datagramSession) {
            sendDatagram(datagramSession, data);
        } else if (session instanceof StreamSession streamSession) {
            sendStream(streamSession, data);
        } else {
            session.close();
        }
    }

    private void sendDatagram(final DatagramSession session, @Nullable final ByteBuffer data) {
        try {
            switch (session.getState()) {
                case NEW -> {
                    final DatagramChannel channel =
                            socketManager.createDatagramChannel(session, readySessions);
                    session.setAttachment(channel);
                    LOGGER.trace("Open datagram socket {}", session.getDestination());
                    // The datagram that opens the exchange has to go out as well. It used to be dropped, so
                    // every first query of a flow, a DNS lookup with its fresh source port, got no answer
                    // until the guest's resolver gave up waiting and asked again.
                    if (data != null) {
                        channel.send(data, session.getDestination());
                    }
                }
                case ESTABLISHED -> {
                    LOGGER.trace("Send datagram");
                    final DatagramChannel channel = SessionChannelHelper.getChannel(session);
                    assert data != null;
                    channel.send(data, session.getDestination());
                }
                case EXPIRED -> {
                    SessionChannelHelper.closeSession(session);
                    LOGGER.trace("Close datagram socket {}", session.getDestination());
                }
                default -> throw new AssertionError(session.getState());
            }
        } catch (IOException e) {
            LOGGER.error("Datagram session failure", e);
            session.close();
        }
    }

    private void sendStream(final StreamSession session, @Nullable final ByteBuffer data) {
        try {
            switch (session.getState()) {
                case NEW -> openStreamSocket(session);
                case ESTABLISHED -> writeEstablishedStream(session, data);
                case FINISH, EXPIRED -> closeEndedStream(session);
                default -> throw new AssertionError(session.getState());
            }
        } catch (IOException e) {
            LOGGER.error("Stream session failure", e);
            session.close();
        }
    }

    /** Opens a non-blocking socket for a brand new stream session. */
    private void openStreamSocket(final StreamSession session) throws IOException {
        final SocketChannel channel = socketManager.createStreamChannel(session, readySessions);
        session.setAttachment(channel);
        channel.connect(session.getDestination());
        if (session instanceof final StreamSessionImpl stream) {
            trackedStreams.add(stream);
        }
        LOGGER.trace("Open stream socket {}", session.getDestination());
    }

    /** Writes the payload to an established, non-blocking socket until EAGAIN or drained. */
    private void writeEstablishedStream(final StreamSession session, final ByteBuffer data)
            throws IOException {
        final SocketChannel channel = SessionChannelHelper.getChannel(session);
        assert data != null;
        while (data.hasRemaining()) {
            if (channel.write(data) == 0) {
                break;
            }
        }
    }

    private static void closeEndedStream(final StreamSession session) {
        SessionChannelHelper.closeSession(session);
        LOGGER.trace("Close stream socket {}", session.getDestination());
    }
}