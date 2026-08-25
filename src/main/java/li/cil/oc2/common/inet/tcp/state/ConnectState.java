package li.cil.oc2.common.inet.tcp.state;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpHeader;
import li.cil.oc2.common.inet.tcp.TcpState;

/**
 * Initial state: the session exists but the VM has not started the three-way handshake yet.
 *
 * <ul>
 *   <li>{@code receive}: emits nothing; nothing can be sent before a SYN arrives.
 *   <li>{@code send}: accepts only a bare SYN ({@code isConnectionInitiation()}), records the
 *       VM's initial sequence number and window, and forwards so that {@link AcceptState} can
 *       answer with SYN-ACK. Any other segment is dropped.
 * </ul>
 *
 * <p>Transition: to {@link AcceptState} once the VM-side session is {@code connect()}ed after the
 * real socket connected; to {@link RejectState} if closed before that happens.
 */
public final class ConnectState extends TcpState {

    @Override
    public SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        return SessionActions.IGNORE;
    }

    @Override
    public SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
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
    public Session.States toSessionState() {
        return Session.States.NEW;
    }
}