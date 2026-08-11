package li.cil.oc2.common.blockentity.projector.video;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import javax.annotation.Nullable;
import li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder;
import li.cil.oc2.jcodec.codecs.h264.encode.CQPRateControl;
import li.cil.oc2.jcodec.common.model.Picture;

public final class ProjectorVideoEncoder {
    private final H264Encoder encoder = new H264Encoder(new CQPRateControl(12));
    private final ByteBuffer encoderBuffer = ByteBuffer.allocateDirect(1024 * 1024);
    boolean needsIDR;

    public ProjectorVideoEncoder() {
        encoder.setKeyInterval(100);
    }

    public void setRequiresKeyframe() {
        needsIDR = true;
    }

    public boolean hasChangesOrNeedsIDR(final ProjectorDevice device) {
        return device.hasChanges() || needsIDR;
    }

    @Nullable
    public ByteBuffer encodeFrame(final ProjectorDevice device, final Picture picture) {
        final boolean hasChanges = device.applyChanges(picture);
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