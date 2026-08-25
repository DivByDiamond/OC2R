package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class FrameCodecTest {
    private final FrameCodec codec = new FrameCodec();

    @Test
    void rawCodecPassthrough() {
        final byte[] frame = new byte[640 * 480 * 2];
        new Random(1).nextBytes(frame);
        final FrameCodec.EncodedFrame encoded = codec.encode(VideoCodec.RAW, frame, 640, 480);
        assertArrayEquals(frame, encoded.data());
        assertEquals(VideoCodec.RAW, encoded.codec());
        final var decoded = codec.decode(VideoCodec.RAW, encoded.data(), 640, 480);
        assertTrue(decoded.isPresent());
        assertArrayEquals(frame, decoded.get());
    }

    @Test
    void h264CompressesSolidColor() {
        final byte[] frame = solidFrame(0xF800);
        final FrameCodec.EncodedFrame encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        assertEquals(VideoCodec.H264, encoded.codec());
        assertTrue(encoded.data().length < frame.length / 8, "solid frame should compress well");
    }

    @Test
    void h264DecodesWithoutCrash() {
        final byte[] frame = solidFrame(0xF800);
        final FrameCodec.EncodedFrame encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        final var decoded = codec.decode(VideoCodec.H264, encoded.data(), 640, 480);
        assertTrue(decoded.isPresent());
        assertEquals(frame.length, decoded.get().length);
    }

    @Test
    void h264SolidColorSurvivesApproximately() {
        final byte[] frame = solidFrame(0xF800); // pure red RGB565
        final FrameCodec.EncodedFrame encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        final var decoded = codec.decode(VideoCodec.H264, encoded.data(), 640, 480);
        assertTrue(decoded.isPresent());
        final byte[] result = decoded.get();
        final int pixel = (result[0] & 0xFF) | (result[1] << 8);
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
        final FrameCodec.EncodedFrame encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        assertTrue(encoded.data().length < frame.length / 8, "sparse text should compress");
        final var decoded = codec.decode(VideoCodec.H264, encoded.data(), 640, 480);
        assertTrue(decoded.isPresent());
    }

    @Test
    void h264DecoderFailureYieldsEmpty() {
        final byte[] garbage = new byte[100];
        new Random(2).nextBytes(garbage);
        final var decoded = codec.decode(VideoCodec.H264, garbage, 640, 480);
        assertFalse(decoded.isPresent());
    }

    @Test
    void h264PayloadIsRawAnnexBNotZlib() {
        final byte[] frame = solidFrame(0xF800);
        final FrameCodec.EncodedFrame encoded = codec.encode(VideoCodec.H264, frame, 640, 480);
        // Annex-B start code of an IDR frame, not a zlib stream header.
        assertEquals(0, encoded.data()[0] & 0xFF);
        assertEquals(0, encoded.data()[1] & 0xFF);
        assertEquals(0, encoded.data()[2] & 0xFF);
        assertEquals(1, encoded.data()[3] & 0xFF);
        final var decoded = codec.decode(VideoCodec.H264, encoded.data(), 640, 480);
        assertTrue(decoded.isPresent());
    }

    @Test
    void deltaCodecRoundtripIsBitExact() {
        final int width = 320;
        final int height = 240;
        final byte[] first = new byte[width * height * 2];
        for (int i = 0; i < width * height; i++) {
            first[i * 2] = (byte) 0xE0;
            first[i * 2 + 1] = 0x07;
        }
        final var encodedFirst = codec.encode(VideoCodec.DELTA, first, width, height);
        assertEquals(VideoCodec.DELTA, encodedFirst.codec());

        final byte[] second = first.clone();
        for (int i = 0; i < 100; i++) {
            second[i * 2 + 1] = 0x1F;
        }
        final var encodedSecond = codec.encode(VideoCodec.DELTA, second, width, height);

        assertArrayEquals(first, codec.decode(VideoCodec.DELTA, encodedFirst.data(), width, height).orElseThrow());
        assertArrayEquals(second, codec.decode(VideoCodec.DELTA, encodedSecond.data(), width, height).orElseThrow());
    }

    @Test
    void videoCodecIdsAreStable() {
        assertEquals(VideoCodec.RAW, VideoCodec.fromId(0));
        assertEquals(VideoCodec.H264, VideoCodec.fromId(1));
        assertEquals(VideoCodec.DELTA, VideoCodec.fromId(2));
        assertEquals(VideoCodec.RAW, VideoCodec.fromId(3));
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
