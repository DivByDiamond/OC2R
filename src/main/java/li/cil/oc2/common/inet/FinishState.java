package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.Session;
import java.nio.ByteBuffer;

final class FinishState extends TcpState {
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
        return Session.States.FINISH;
    }
}
