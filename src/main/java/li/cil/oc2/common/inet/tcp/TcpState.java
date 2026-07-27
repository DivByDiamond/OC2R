package li.cil.oc2.common.inet.tcp;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.inet.session.SessionActions;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;

public abstract class TcpState {
    abstract SessionActions receive(StreamSessionImpl session, ByteBuffer segment);

    abstract SessionActions send(StreamSessionImpl session, ByteBuffer segment);

    abstract Session.States toSessionState();
}