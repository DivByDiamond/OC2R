package li.cil.oc2.common.blockentity.monitor.video;

import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.HEIGHT;
import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.WIDTH;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.monitor.misc.FrameConsumer;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.monitor.framebuffer.MonitorRequestFramebufferMessage;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;

public final class MonitorVideoController {

    private final ReentrantLock lock = new ReentrantLock();

    final Picture picture = Picture.create(WIDTH, HEIGHT, ColorSpace.YUV420J);
    @Nullable FrameConsumer frameConsumer;
    final MonitorVideoEncoder encoder = new MonitorVideoEncoder();
    final MonitorVideoDecoder decoder = new MonitorVideoDecoder();
    private long lastKeepAliveSentAt;
    private final MonitorBlockEntity monitor;

    public MonitorVideoController(final MonitorBlockEntity monitor) {
        this.monitor = monitor;
    }

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        if (Objects.equals(consumer, frameConsumer)) return;
        lock.lock();
        try {

            this.frameConsumer = consumer;
            if (frameConsumer != null) frameConsumer.processFrame(picture);
        
        } finally {
            lock.unlock();
        }
    }

    public void setRequiresKeyframe() {
        encoder.setRequiresKeyframe();
    }

    public boolean isKeyframeRequired() {
        return encoder.isKeyframeRequired();
    }

    public void applyNextFrameClient(final ByteBuffer frameData) {
        decoder.applyNextFrameClient(frameData, picture, frameConsumer);
    }

    public void onRendering() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > 1000) {
            lastKeepAliveSentAt = now;
            NetworkMessages.sendToServer(new MonitorRequestFramebufferMessage(monitor));
        }
    }

    public void clearPicture() {
        Arrays.fill(picture.getPlaneData(0), (byte) -128);
    }

    @Nullable
    public ByteBuffer encodeFrame(final MonitorDevice monitorDevice) {
        return encoder.encodeFrame(picture, monitorDevice);
    }
}
