package li.cil.oc2.common.inet;

import java.nio.ByteBuffer;
import li.cil.oc2.api.inet.session.Session;

abstract class TcpState {
    abstract SessionActions receive(StreamSessionImpl session, ByteBuffer segment);

    abstract SessionActions send(StreamSessionImpl session, ByteBuffer segment);

    abstract Session.States toSessionState();
}