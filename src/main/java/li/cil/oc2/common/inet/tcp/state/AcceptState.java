package li.cil.oc2.common.inet.tcp.state;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpHeader;
import li.cil.oc2.common.inet.tcp.TcpState;
import li.cil.oc2.common.inet.tcp.TcpStates;

public final class AcceptState extends TcpState {
    @Override
    SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        header.acceptConnection(
                session.mySequence, session.vmSequence + 1, session.computeWindow());
        header.write(segment);
        segment.flip();
        return SessionActions.FORWARD;
    }

    @Override
    SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        if (!header.read(segment)) {
            return SessionActions.IGNORE;
        }
        if (!header.isAcceptanceOrRejectionAcknowledged()) {
            return SessionActions.IGNORE;
        }
        session.mySequence += 1;
        session.vmSequence += 1;
        session.state = TcpStates.ESTABLISHED;
        session.vmWindow = header.window;
        return SessionActions.IGNORE;
    }

    @Override
    Session.States toSessionState() {
        return Session.States.ESTABLISHED;
    }
}