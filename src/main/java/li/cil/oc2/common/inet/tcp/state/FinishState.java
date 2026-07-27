package li.cil.oc2.common.inet.tcp.state;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpState;

public final class FinishState extends TcpState {
    @Override
    public SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        return SessionActions.DROP;
    }

    @Override
    public SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
        return SessionActions.DROP;
    }

    @Override
    public Session.States toSessionState() {
        return Session.States.FINISH;
    }
}