package li.cil.oc2.common.inet.session.manager;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import li.cil.oc2.api.inet.InternetManager;
import li.cil.oc2.api.inet.session.DatagramSession;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.inet.session.manager.ready.ReadySessions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reference-counted holder of the single NIO {@link Selector} shared by all session layers
 * (one per mounted internet card). Each layer must {@link #attach} on construction and
 * {@link #detach} on teardown; the selector and its internet-thread tick task live until the last
 * user detaches.
 *
 * <p>Every internet-thread tick, {@code selectNow()} scans the registered channels and enqueues
 * ready sessions into the owning layer's {@link ReadySessions} queues, which the session layer
 * then drains one session per pass from {@code receiveSession(Receiver)}.
 */
public final class SocketManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static int socketManagerUsesCount = 0;
    private static SocketManager managerInstance = null;

    /**
     * Registers a user of the shared selector. The first call creates the manager (and schedules
     * its selection task); subsequent calls only bump the use count. Must be paired with exactly
     * one {@link #detach()} call.
     */
    public static SocketManager attach(final InternetManager internetManager) {
        final int oldCount = socketManagerUsesCount;
        socketManagerUsesCount++;
        if (oldCount == 0) {
            assert managerInstance == null;
            managerInstance = new SocketManager(internetManager);
        }
        assert managerInstance != null;
        return managerInstance;
    }

    private final Selector selector;
    private final InternetManager.Task selectionTask;

    /** Runs once per internet-thread tick: collects currently ready sessions into queues. */
    private void selectionTaskFunction() {
        try {
            selector.selectNow(
                    selectionKey -> {
                        final ChannelAttachment attachment =
                                (ChannelAttachment) selectionKey.attachment();
                        final Session session = attachment.session;
                        final ReadySessions readySessions = attachment.readySessions;
                        if (selectionKey.isReadable()) {
                            readySessions.getToRead().add(session);
                        }
                        if (selectionKey.isWritable()) {
                            readySessions.getToWrite().add(session);
                        }
                        if (selectionKey.isConnectable()) {
                            readySessions.getToConnect().add(session);
                        }
                    });
        } catch (final IOException exception) {
            LOGGER.error("Exception while selecting", exception);
        }
    }

    private SocketManager(final InternetManager internetManager) {
        try {
            selector = Selector.open();
        } catch (final IOException exception) {
            throw new Error("Failed to open selector", exception);
        }
        selectionTask = internetManager.runOnInternetThreadTick(this::selectionTaskFunction);
        LOGGER.info("Started socket manager");
    }

    /** Links a registered channel back to its session and the layer's readiness queues. */
    private record ChannelAttachment(Session session, ReadySessions readySessions) {}

    /** Opens a non-blocking datagram channel registered for read and write readiness. */
    public DatagramChannel createDatagramChannel(
            final DatagramSession session, final ReadySessions readySessions) throws IOException {
        final DatagramChannel datagramChannel = DatagramChannel.open();
        datagramChannel.configureBlocking(false);
        final ChannelAttachment attachment = new ChannelAttachment(session, readySessions);
        final int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        datagramChannel.register(selector, ops, attachment);
        return datagramChannel;
    }

    /** Opens a non-blocking socket channel registered for read, write and connect readiness. */
    public SocketChannel createStreamChannel(
            final StreamSession session, final ReadySessions readySessions) throws IOException {
        final SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        final ChannelAttachment attachment = new ChannelAttachment(session, readySessions);
        final int ops = SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT;
        socketChannel.register(selector, ops, attachment);
        return socketChannel;
    }

    private void shutdown() {
        selectionTask.close();
        try {
            selector.close();
        } catch (final IOException exception) {
            LOGGER.error("Exception during socket manager shutdown", exception);
        }
        LOGGER.info("Stopped socket manager");
    }

    public void detach() {
        socketManagerUsesCount--;
        if (socketManagerUsesCount == 0) {
            shutdown();
            managerInstance = null;
        }
    }
}