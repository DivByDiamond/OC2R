package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.Session;

import java.nio.ByteBuffer;

abstract class TcpState {
    abstract SessionActions receive(StreamSessionImpl session, ByteBuffer segment);

    abstract SessionActions send(StreamSessionImpl session, ByteBuffer segment);

    abstract Session.States toSessionState();
}
