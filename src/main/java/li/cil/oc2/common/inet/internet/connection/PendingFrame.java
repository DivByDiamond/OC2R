package li.cil.oc2.common.inet.internet.connection;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

public final class PendingFrame {
    private final AtomicReference<byte[]> frameRef = new AtomicReference<>();

    @Nullable
    public byte[] get() {
        return frameRef.getAndSet(null);
    }

    public void put(final byte[] frame) {
        frameRef.set(frame);
    }
}