package li.cil.oc2.common.blockentity.monitor.video;

import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.HEIGHT;
import static li.cil.oc2.common.bus.device.vm.block.MonitorDevice.WIDTH;
import static li.cil.oc2.common.vm.device.SimpleFramebufferDevice.STRIDE;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import javax.annotation.Nullable;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder;
import li.cil.oc2.jcodec.codecs.h264.encode.CQPRateControl;
import li.cil.oc2.jcodec.common.model.Picture;

final class MonitorVideoEncoder {
    private final H264Encoder encoder = new H264Encoder(new CQPRateControl(12));
    private final ByteBuffer encoderBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * STRIDE);
    private boolean needsIDR;

    MonitorVideoEncoder() {
        encoder.setKeyInterval(100);
    }

    void setRequiresKeyframe() {
        needsIDR = true;
    }

    boolean isKeyframeRequired() {
        return needsIDR;
    }

    @Nullable
    ByteBuffer encodeFrame(final Picture picture, final MonitorDevice monitorDevice) {
        final boolean hasChanges = monitorDevice.applyChanges(picture);
        if (!hasChanges && !needsIDR) {
            return null;
        }

        encoderBuffer.clear();
        final ByteBuffer frameData;
        try {
            if (needsIDR) {
                frameData = encoder.encodeIDRFrame(picture, encoderBuffer);
                needsIDR = false;
            } else {
                frameData = encoder.encodeFrame(picture, encoderBuffer).data();
            }
        } catch (final BufferOverflowException ignored) {
            return null;
        }

        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(frameData);
        deflater.finish();
        final ByteBuffer compressedFrameData = ByteBuffer.allocateDirect(1024 * 1024);
        deflater.deflate(compressedFrameData, Deflater.FULL_FLUSH);
        deflater.end();
        compressedFrameData.flip();

        return compressedFrameData;
    }
}