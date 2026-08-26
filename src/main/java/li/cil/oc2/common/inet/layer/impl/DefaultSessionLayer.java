package li.cil.oc2.common.inet.layer.impl;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
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

    private final AtomicReference<EchoResponse> echoResponse = new AtomicReference<>(null);

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
            final ByteBuffer datagram = receiver.receive(datagramSession);
            assert datagram != null;
            final SocketAddress address = channel.receive(datagram);
            if (address == null) {
                return false;
            }
            if (Config.useSynchronisedNAT
                    && !address.equals(datagramSession.getDestination())) {
                // With synchronised NAT the socket is bound to a wildcard address, so drop
                // datagrams that did not come from the session's expected destination.
                return false;
            }
            datagram.flip();
            return true;
        } catch (final IOException exception) {
            LOGGER.error("Trying to read datagram socket", exception);
        }
        return false;
    }

    private boolean readStream(final Receiver receiver, final StreamSession streamSession) {
        LOGGER.trace("Stream received");
        try {
            final SocketChannel channel = SessionChannelHelper.getChannel(streamSession);
            final ByteBuffer stream = receiver.receive(streamSession);
            assert stream != null;
            // Non-blocking channel: read until EAGAIN (0), end of stream (-1)
            // or the session buffer is full to make use of the whole window.
            for (;;) {
                final int read = channel.read(stream);
                if (read <= 0) {
                    if (read == -1) {
                        SessionChannelHelper.closeSession(streamSession);
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
            } else if (stream.receiveBuffer.position() > stream.nextSegmentMark) {
                receiver.receive(stream);
                return;
            }
        }
    }

    public static native byte @Nullable [] sendICMP(byte[] ip, byte[] data, int size, int timeout);

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