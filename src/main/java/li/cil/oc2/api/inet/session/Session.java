package li.cil.oc2.api.inet.session;

import java.net.InetSocketAddress;
import java.time.Instant;
import javax.annotation.Nullable;

public interface Session {
    long getId();

    void close();

    States getState();

    @Nullable
    Object getAttachment();

    void setAttachment(@Nullable final Object userdata);

    InetSocketAddress getDestination();

    Instant getLastUpdateTime();

    default boolean isClosed() {
        final States state = getState();
        return state == States.FINISH || state == States.REJECT || state == States.EXPIRED;
    }

    enum States {
        NEW,
        ESTABLISHED,
        FINISH,
        REJECT,
        EXPIRED
    }
}