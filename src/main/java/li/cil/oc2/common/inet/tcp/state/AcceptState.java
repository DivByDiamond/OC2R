package li.cil.oc2.common.inet.tcp.state;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;
import li.cil.oc2.common.inet.tcp.TcpHeader;
import li.cil.oc2.common.inet.tcp.TcpState;
import li.cil.oc2.common.inet.tcp.TcpStates;

/**
 * Handshake state: the VM's SYN has been seen and the real socket is (being) connected.
 *
 * <ul>
 *   <li>{@code receive}: emits the SYN-ACK ({@code acceptConnection()}) acknowledging
 *       {@code vmSequence + 1}; retransmitted on every pass until the VM completes the handshake.
 *   <li>{@code send}: waits for a bare ACK ({@code isAcceptanceOrRejectionAcknowledged()}),
 *       consumes the sequence numbers occupied by the exchanged SYNs, latches {@code vmWindow}
 *       and moves to {@link EstablishedState}. Returns IGNORE since an ACK needs no answer.
 * </ul>
 */
public final class AcceptState extends TcpState {
    @Override
    public SessionActions receive(final StreamSessionImpl session, final ByteBuffer segment) {
        final TcpHeader header = session.header;
        header.acceptConnection(
                session.mySequence, session.vmSequence + 1, session.computeWindow());
        header.write(segment);
        segment.flip();
        return SessionActions.FORWARD;
    }

    @Override
    public SessionActions send(final StreamSessionImpl session, final ByteBuffer segment) {
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
    public Session.States toSessionState() {
        return Session.States.ESTABLISHED;
    }
}