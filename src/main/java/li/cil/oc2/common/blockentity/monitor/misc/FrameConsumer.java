package li.cil.oc2.common.blockentity.monitor.misc;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface FrameConsumer {
    void processFrame(final int width, final int height, final ByteBuffer rgb565);
}
