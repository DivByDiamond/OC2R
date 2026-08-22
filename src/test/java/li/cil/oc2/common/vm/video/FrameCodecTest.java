package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class FrameCodecTest {
    private final FrameCodec codec = new FrameCodec();

    @Test
    void rawCodecPassthrough() {
        final byte[] frame = new byte[640 * 480 * 2];
        new Random(1).nextBytes(frame);
        final byte[] encoded = codec.encode(VideoCodec.RAW, frame, 640, 480);
        assertArrayEquals(frame, encoded);
        final byte[] decoded = codec.decode(VideoCodec.RAW, encoded, 640, 480);
        assertArrayEquals(frame, decoded);
    }

    @Test
    void h264CompressesSolidColor() {
        final byte[] frame = solidFrame(0xF800);
        final byte[] encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        assertNotNull(encoded);
        assertTrue(encoded.length < frame.length / 8, "solid frame should compress well");
    }

    @Test
    void h264DecodesWithoutCrash() {
        final byte[] frame = solidFrame(0xF800);
        final byte[] encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        assertNotNull(encoded);
        final byte[] decoded = codec.decode(VideoCodec.H264, encoded, 640, 480);
        assertNotNull(decoded);
        assertEquals(frame.length, decoded.length);
    }

    @Test
    void h264SolidColorSurvivesApproximately() {
        final byte[] frame = solidFrame(0xF800); // pure red RGB565
        final byte[] encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        assertNotNull(encoded);
        final byte[] decoded = codec.decode(VideoCodec.H264, encoded, 640, 480);
        assertNotNull(decoded);
        final int pixel = (decoded[0] & 0xFF) | (decoded[1] << 8);
        final int r5 = (pixel >>> 11) & 0x1F;
        assertTrue(r5 >= 27, "red should be near max, got r5=" + r5);
    }

    @Test
    void h264SparseTextCompresses() {
        final byte[] frame = new byte[640 * 480 * 2];
        for (int y = 0; y < 480; y++) {
            for (int x = 0; x < 640; x++) {
                final boolean glyph = ((y / 8) % 2 == 0) && ((x / 8) % 2 == 0);
                final int pixel = glyph ? 0xFFFF : 0x0000;
                frame[(y * 640 + x) * 2] = (byte) (pixel & 0xFF);
                frame[(y * 640 + x) * 2 + 1] = (byte) (pixel >>> 8);
            }
        }
        final byte[] encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        assertNotNull(encoded);
        assertTrue(encoded.length < frame.length / 8, "sparse text should compress");
        final byte[] decoded = codec.decode(VideoCodec.H264, encoded, 640, 480);
        assertNotNull(decoded);
    }

    private static byte[] solidFrame(final int pixel) {
        final byte[] frame = new byte[640 * 480 * 2];
        for (int i = 0; i < frame.length; i += 2) {
            frame[i] = (byte) (pixel & 0xFF);
            frame[i + 1] = (byte) (pixel >>> 8);
        }
        return frame;
    }
}
