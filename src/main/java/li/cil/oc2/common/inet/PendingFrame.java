package li.cil.oc2.common.inet;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

final class PendingFrame {
    private final AtomicReference<byte[]> pendingFrame = new AtomicReference<>();

    @Nullable
    public byte[] get() {
        return pendingFrame.getAndSet(null);
    }

    public void put(final byte[] frame) {
        pendingFrame.set(frame);
    }
}
