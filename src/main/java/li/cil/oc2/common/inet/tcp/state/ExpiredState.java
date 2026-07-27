package li.cil.oc2.common.inet.tcp.state;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpState;

public final class ExpiredState extends TcpState {
    @Override
    SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        return SessionActions.DROP;
    }

    @Override
    SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
        return SessionActions.DROP;
    }

    @Override
    Session.States toSessionState() {
        return Session.States.EXPIRED;
    }
}