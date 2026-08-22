package li.cil.oc2.common.vm.video;

import java.io.ByteArrayOutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import li.cil.oc2.jcodec.codecs.h264.H264Decoder;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder;
import li.cil.oc2.jcodec.codecs.h264.encode.CQPRateControl;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.RgbToYuv420j;
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;

public final class FrameCodec {
    private static final int KEY_INTERVAL = 100;
    private static final int ENCODER_BUFFER_SIZE = 4 * 1024 * 1024;

    public record EncodedFrame(VideoCodec codec, byte[] data) {}

    private final H264Encoder h264Encoder = new H264Encoder(new CQPRateControl(12));
    private final H264Decoder h264Decoder = new H264Decoder();
    private final ByteBuffer encoderBuffer = ByteBuffer.allocateDirect(ENCODER_BUFFER_SIZE);
    private Picture encoderPicture = Picture.create(0, 0, ColorSpace.YUV420J);
    private boolean needsIDR = true;

    public FrameCodec() {
        h264Encoder.setKeyInterval(KEY_INTERVAL);
    }

    public EncodedFrame encode(
            final VideoCodec codec, final byte[] rgb565, final int width, final int height) {
        if (codec != VideoCodec.H264) {
            return new EncodedFrame(VideoCodec.RAW, rgb565);
        }

        encoderPicture = ensurePicture(encoderPicture, width, height);
        convertRgb565ToYuv(rgb565, width, height, encoderPicture);

        encoderBuffer.clear();
        final ByteBuffer frameData;
        try {
            if (needsIDR) {
                frameData = h264Encoder.encodeIDRFrame(encoderPicture, encoderBuffer);
                needsIDR = false;
            } else {
                frameData = h264Encoder.encodeFrame(encoderPicture, encoderBuffer).data();
            }
        } catch (final BufferOverflowException ignored) {
            return new EncodedFrame(VideoCodec.RAW, rgb565);
        }

        return new EncodedFrame(VideoCodec.H264, deflate(frameData));
    }

    public Optional<byte[]> decode(
            final VideoCodec codec, final byte[] data, final int width, final int height) {
        if (codec != VideoCodec.H264) {
            return Optional.of(data);
        }

        try {
            final Optional<ByteBuffer> inflated = inflate(data);
            if (inflated.isEmpty()) {
                return Optional.empty();
            }

            final Picture yuv = Picture.create(width, height, ColorSpace.YUV420J);
            h264Decoder.decodeFrame(inflated.get(), yuv.getData());
            return Optional.of(convertYuvToRgb565(yuv, width, height));
        } catch (final Exception ignored) {
            return Optional.empty();
        }
    }

    private static Picture ensurePicture(
            final Picture current, final int width, final int height) {
        if (current.getWidth() != width || current.getHeight() != height) {
            return Picture.create(width, height, ColorSpace.YUV420J);
        }
        return current;
    }

    private static byte[] deflate(final ByteBuffer input) {
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(input);
        deflater.finish();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            final int length = deflater.deflate(buffer);
            out.write(buffer, 0, length);
        }
        deflater.end();
        return out.toByteArray();
    }

    private static Optional<ByteBuffer> inflate(final byte[] data) {
        final Inflater inflater = new Inflater();
        inflater.setInput(data);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        try {
            while (!inflater.finished()) {
                final int length = inflater.inflate(buffer);
                if (length == 0) {
                    break;
                }
                out.write(buffer, 0, length);
            }
            inflater.end();
            return Optional.of(ByteBuffer.wrap(out.toByteArray()));
        } catch (final DataFormatException e) {
            return Optional.empty();
        }
    }

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
