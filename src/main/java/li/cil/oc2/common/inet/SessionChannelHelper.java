package li.cil.oc2.common.inet;

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

final class SessionChannelHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    static boolean processQueue(
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

    static void closeSession(final Session session) {
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

    static SocketChannel getChannel(final StreamSession session) {
        return (SocketChannel) getExistingUserdata(session);
    }

    static DatagramChannel getChannel(final DatagramSession session) {
        return (DatagramChannel) getExistingUserdata(session);
    }

    static SelectableChannel getChannel(final Session session) {
        return (SelectableChannel) getExistingUserdata(session);
    }
}