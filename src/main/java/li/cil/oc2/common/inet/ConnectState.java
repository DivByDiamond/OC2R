package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

final class ConnectState extends TcpState {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        return SessionActions.IGNORE;
    }

    @Override
    SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        if (!header.read(segment)) {
            return SessionActions.DROP;
        }
        if (!header.isConnectionInitiation()) {
            return SessionActions.DROP;
        }
        session.vmSequence = header.sequenceNumber;
        session.vmWindow = header.window;
        return SessionActions.FORWARD;
    }

    @Override
    Session.States toSessionState() {
        return Session.States.NEW;
    }
}
