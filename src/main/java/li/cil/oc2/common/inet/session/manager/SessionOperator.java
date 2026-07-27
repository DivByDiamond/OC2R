package li.cil.oc2.common.inet.session.manager;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import li.cil.oc2.api.inet.session.Session;

public public interface SessionOperator extends Session {
    @Nullable
    byte[] nextReceive();

    void nextSent(final byte[] data);

    default void nextSent(final ByteBuffer data) {
        final byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        nextSent(bytes);
    }
}