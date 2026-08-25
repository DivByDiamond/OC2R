package li.cil.oc2.common.inet.tcp.state.finish;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpHeader;
import li.cil.oc2.common.inet.tcp.TcpState;

/**
 * Terminal state for refused connections (closed before the handshake completed, or the real
 * connection attempt failed).
 *
 * <ul>
 *   <li>{@code receive}: emits a RST ({@code rejectConnection()}) towards the VM; retransmitted
 *       until the manager closes the session after the first forwarded RST.
 *   <li>{@code send}: unreachable — no further VM segments are processed once the RST has been
 *       emitted, hence the exception.
 * </ul>
 */
public final class RejectState extends TcpState {
    @Override
    public SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        header.rejectConnection(session.mySequence, session.vmSequence + 1);
        header.write(segment);
        segment.flip();
        return SessionActions.FORWARD;
    }

    @Override
    public SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
        throw new IllegalStateException();
    }

    @Override
    public Session.States toSessionState() {
        return Session.States.REJECT;
    }
}