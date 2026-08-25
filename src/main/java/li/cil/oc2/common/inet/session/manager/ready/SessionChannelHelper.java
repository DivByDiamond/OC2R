package li.cil.oc2.common.inet.session.manager.ready;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.function.Function;
import li.cil.oc2.api.inet.session.DatagramSession;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.api.inet.session.StreamSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Static helpers tying sessions to the NIO channels stored in their {@code attachment}, plus
 * draining of the {@link ReadySessions} readiness queues.
 */
public final class SessionChannelHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Drains the given readiness queue, skipping closed sessions, until {@code action} succeeds
     * for one session or the queue is exhausted. Returns whether an action was applied. This
     * limits the layer to one I/O-ready session per pass, since a single message buffer can only
     * serve one of them.
     */
    public static boolean processQueue(
            final Queue<Session> queue, final Function<Session, Boolean> action) {
        while (true) {
            final Session session = queue.poll();
            if (session == null) {
                return false;
            }
            if (session.isClosed()) {
                continue;
            }
            if (action.apply(session)) {
                return true;
            }
        }
    }

    /**
     * Closes the session's channel and marks the session closed (unless already closed), which
     * makes the transport layer discard it and emit any pending RST/FIN towards the VM.
     */
    public static void closeSession(final Session session) {
        try {
            getChannel(session).close();
            if (!session.isClosed()) {
                session.close();
            }
        } catch (final IOException exception) {
            LOGGER.error("Error on closing channel", exception);
        }
    }

    private static Object getExistingUserdata(final Session session) {
        final Object channel = session.getAttachment();
        assert channel != null;
        return channel;
    }

    public static SocketChannel getChannel(final StreamSession session) {
        return (SocketChannel) getExistingUserdata(session);
    }

    public static DatagramChannel getChannel(final DatagramSession session) {
        return (DatagramChannel) getExistingUserdata(session);
    }

    public static SelectableChannel getChannel(final Session session) {
        return (SelectableChannel) getExistingUserdata(session);
    }
}