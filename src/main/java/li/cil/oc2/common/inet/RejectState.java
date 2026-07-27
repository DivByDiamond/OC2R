package li.cil.oc2.common.inet;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;

final class RejectState extends TcpState {
    @Override
    SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        header.rejectConnection(session.mySequence, session.vmSequence + 1);
        header.write(segment);
        segment.flip();
        return SessionActions.FORWARD;
    }

    @Override
    SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
        throw new IllegalStateException();
    }

    @Override
    Session.States toSessionState() {
        return Session.States.REJECT;
    }
}