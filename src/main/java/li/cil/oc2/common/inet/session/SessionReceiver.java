package li.cil.oc2.common.inet.session;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.DatagramSession;
import li.cil.oc2.api.inet.session.EchoSession;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;

public final class SessionReceiver implements SessionLayer.Receiver {
    public SessionBase session = null;
    ByteBuffer buffer = null;
    int position = 0;
    int limit = 0;

    public void prepare(final ByteBuffer buffer) {
        session = null;
        this.buffer = buffer;
        position = buffer.position();
        limit = buffer.limit();
    }

    public ByteBuffer getBuffer() {
        buffer.position(position);
        return buffer;
    }

    @Nullable
    @Override
    public ByteBuffer receive(final Session session) {
        buffer.position(position);
        buffer.limit(limit);
        this.session = (SessionBase) session;
        return switch (session.getState()) {
            case NEW, FINISH, REJECT -> null;
            case ESTABLISHED -> receiveEstablished(session);
            default -> throw new IllegalStateException();
        };
    }

    private ByteBuffer receiveEstablished(final Session session) {
        if (session instanceof EchoSession || session instanceof DatagramSession) {
            buffer.putLong(0);
            return buffer;
        } else if (session instanceof StreamSession) {
            final StreamSessionImpl stream = (StreamSessionImpl) session;
            return stream.getReceiveBuffer();
        } else {
            throw new IllegalArgumentException("session");
        }
    }
}