package li.cil.oc2.common.vm.video;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Optional;
import li.cil.oc2.jcodec.codecs.h264.H264Decoder;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder;
import li.cil.oc2.jcodec.codecs.h264.encode.CQPRateControl;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.RgbToYuv420j;
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;
import org.jetbrains.annotations.Nullable;

public class FrameCodec {
    private static final int KEY_INTERVAL = 100;
    private static final int ENCODER_BUFFER_SIZE = 4 * 1024 * 1024;

    @SuppressWarnings("ArrayRecordComponent")
    public record EncodedFrame(VideoCodec codec, byte[] data) {}

    // Encoder/decoder state is allocated lazily: most BlockEntities never send
    // or receive an H264 frame (RAW is the default codec), so they should not
    // pay the 4MB direct buffer plus jcodec structures up front.
    @Nullable private H264Encoder h264Encoder;
    @Nullable private H264Decoder h264Decoder;
    @Nullable private ByteBuffer encoderBuffer;
    @Nullable private Picture encoderPicture;
    private boolean needsIDR = true;

    @Nullable private DeltaFrameCodec deltaCodec;

    public EncodedFrame encode(
            final VideoCodec codec, final byte[] rgb565, final int width, final int height) {
        switch (codec) {
            case RAW -> {
                return new EncodedFrame(VideoCodec.RAW, rgb565);
            }
            case DELTA -> {
                if (deltaCodec == null) {
                    deltaCodec = new DeltaFrameCodec();
                }
                return new EncodedFrame(VideoCodec.DELTA, deltaCodec.encode(rgb565, width,
                        height));
            }
            default -> {}
        }

        if (h264Encoder == null) {
            h264Encoder = new H264Encoder(new CQPRateControl(12));
            h264Encoder.setKeyInterval(KEY_INTERVAL);
        }
        encoderPicture = ensurePicture(encoderPicture, width, height);
        convertRgb565ToYuv(rgb565, width, height, encoderPicture);

        if (encoderBuffer == null) {
            encoderBuffer = ByteBuffer.allocateDirect(ENCODER_BUFFER_SIZE);
        }
        final ByteBuffer buffer = encoderBuffer;
        buffer.clear();
        final ByteBuffer frameData;
        try {
            // SPS/PPS are only emitted in IDR frames, so the first encoded frame
            // must be an IDR for clients to pick up the stream parameters.
            if (needsIDR) {
                frameData = h264Encoder.encodeIDRFrame(encoderPicture, buffer);
                needsIDR = false;
            } else {
                frameData = h264Encoder.encodeFrame(encoderPicture, buffer).data();
            }
        } catch (final BufferOverflowException ignored) {
            return new EncodedFrame(VideoCodec.RAW, rgb565);
        }

        return new EncodedFrame(VideoCodec.H264, toArray(frameData));
    }

    public Optional<byte[]> decode(
            final VideoCodec codec, final byte[] data, final int width, final int height) {
        if (codec == VideoCodec.RAW) {
            return Optional.of(data);
        }
        if (codec == VideoCodec.DELTA) {
            if (deltaCodec == null) {
                deltaCodec = new DeltaFrameCodec();
            }
            return deltaCodec.decode(data, width, height);
        }

        try {
            if (!hasAnnexBStartCode(data)) {
                return Optional.empty();
            }
            if (h264Decoder == null) {
                h264Decoder = new H264Decoder();
            }
            final Picture yuv = Picture.create(width, height, ColorSpace.YUV420J);
            h264Decoder.decodeFrame(ByteBuffer.wrap(data), yuv.getData());
            return Optional.of(convertYuvToRgb565(yuv, width, height));
        } catch (final Exception ignored) {
            return Optional.empty();
        }
    }

    // Encoded H264 payloads are always Annex-B byte streams; rejecting anything
    // else up front avoids spinning up a decoder (and swallowing its exceptions)
    // for garbage or truncated input.
    private static boolean hasAnnexBStartCode(final byte[] data) {
        if (data.length < 4) {
            return false;
        }
        return (data[0] == 0 && data[1] == 0 && data[2] == 0 && data[3] == 1)
                || (data[0] == 0 && data[1] == 0 && data[2] == 1);
    }

    private static byte[] toArray(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private static Picture ensurePicture(
            @Nullable final Picture current, final int width, final int height) {
        if (current == null || current.getWidth() != width || current.getHeight() != height) {
            return Picture.create(width, height, ColorSpace.YUV420J);
        }
        return current;
    }

    /**
     * Converts a YUV420J picture back to little-endian RGB565.
     *
     * <p>{@code Yuv420jToRgb.YUVJtoRGB} works with samples centered at zero and
     * returns them as signed bytes, so "+128" restores the unsigned 0..255 range
     * before each channel is truncated to its 5/6-bit RGB565 width. Chroma is
     * shared by all pixels of a 2x2 block (4:2:0 subsampling), hence the halved
     * chroma plane indexing.
     */
    private static byte[] convertYuvToRgb565(
            final Picture yuv, final int width, final int height) {
        final byte[] y = yuv.getPlaneData(0);
        final byte[] u = yuv.getPlaneData(1);
        final byte[] v = yuv.getPlaneData(2);
        final byte[] rgb = new byte[width * height * 2];
        final byte[] tmp = new byte[3];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                final int index = row * width + col;
                final int uIndex = row / 2 * (width / 2) + col / 2;
                final byte cb = u[uIndex];
                final byte cr = v[uIndex];
                Yuv420jToRgb.YUVJtoRGB(y[index], cb, cr, tmp, 0);
                final int r = (tmp[0] + 128) & 0xFF;
                final int g = (tmp[1] + 128) & 0xFF;
                final int b = (tmp[2] + 128) & 0xFF;
                final int pixel = r >> 3 << 11 | g >> 2 << 5 | b >> 3;
                rgb[index * 2] = (byte) (pixel & 0xFF);
                rgb[index * 2 + 1] = (byte) (pixel >>> 8);
            }
        }
        return rgb;
    }

    /**
     * Packs a little-endian RGB565 framebuffer into jcodec's YUV420J picture.
     *
     * <p>The 5/6-bit channels are scaled up to the full 0..255 range, then biased
     * by -128 because {@code RgbToYuv420j.rgb2yuv} expects samples centered at
     * zero (it re-adds 128 internally). Chroma is written once per 2x2 block
     * (4:2:0 subsampling), sampled from the block's top-left pixel rather than
     * averaged — cheap and good enough for H264's own chroma smoothing.
     */
    private static void convertRgb565ToYuv(
            final byte[] rgb565, final int width, final int height, final Picture yuv) {
        final byte[] y = yuv.getPlaneData(0);
        final byte[] u = yuv.getPlaneData(1);
        final byte[] v = yuv.getPlaneData(2);
        final int[] out = new int[3];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                final int index = row * width + col;
                final int pixel = (rgb565[index * 2] & 0xFF) | (rgb565[index * 2 + 1] << 8);
                final int r5 = (pixel >>> 11) & 0x1F;
                final int g6 = (pixel >>> 5) & 0x3F;
                final int b5 = pixel & 0x1F;
                final byte r = (byte) ((r5 * 255 / 0x1F) - 128);
                final byte g = (byte) ((g6 * 255 / 0x3F) - 128);
                final byte b = (byte) ((b5 * 255 / 0x1F) - 128);

                RgbToYuv420j.rgb2yuv(r, g, b, out);

                y[index] = (byte) out[0];
                if ((row & 1) == 0 && (col & 1) == 0) {
                    final int uvIndex = row / 2 * (width / 2) + col / 2;
                    u[uvIndex] = (byte) out[1];
                    v[uvIndex] = (byte) out[2];
                }
            }
        }
    }
}
