package li.cil.oc2.common.inet.tcp;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;

/**
 * A node of the TCP state machine driven by {@link StreamSessionImpl#state}.
 *
 * <p>Note the inverted naming: {@link #receive} is called when the stack prepares a segment to be
 * handed <em>to</em> the VM, while {@link #send} is called with a segment that arrived <em>from</em>
 * the VM and must be validated before its payload is accepted.
 */
public abstract class TcpState {
    public abstract SessionActions receive(StreamSessionImpl session, ByteBuffer segment);

    public abstract SessionActions send(StreamSessionImpl session, ByteBuffer segment);

    public abstract Session.States toSessionState();
}