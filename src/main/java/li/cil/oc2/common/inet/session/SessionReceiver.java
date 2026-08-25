package li.cil.oc2.common.inet.session;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.DatagramSession;
import li.cil.oc2.api.inet.session.EchoSession;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.inet.session.stream.StreamSessionImpl;

/**
 * The {@link SessionLayer.Receiver} used by the transport layer when interrogating the session
 * layer. It doubles as the result carrier: after {@link #receive} has been called by the session
 * layer, {@link #session} holds the accepted session and {@link #getBuffer()} rewinds the message
 * buffer to the payload start so the transport layer can write protocol headers over it.
 *
 * <p>Contract of {@link #receive(Session)}: it resets the buffer to the remembered
 * position/limit of the current transport message, records the given session, and returns the
 * buffer in which the session layer must place the incoming payload — or {@code null} when there
 * is nothing to hand over (sessions in NEW/FINISH/REJECT states). For echo and datagram sessions
 * eight bytes are reserved as scratch space for the ICMP/UDP header that the transport layer
 * writes afterwards; for stream sessions the session's own receive buffer is returned instead.
 */
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