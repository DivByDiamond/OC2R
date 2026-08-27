package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class DeltaFrameCodecTest {
    private final DeltaFrameCodec encoder = new DeltaFrameCodec();
    private final DeltaFrameCodec decoder = new DeltaFrameCodec();

    private byte[] frame(final int width, final int height, final java.util.function.IntUnaryOperator pixel) {
        final byte[] frame = new byte[width * height * 2];
        for (int i = 0; i < width * height; i++) {
            final int value = pixel.applyAsInt(i);
            frame[i * 2] = (byte) (value & 0xFF);
            frame[i * 2 + 1] = (byte) ((value >>> 8) & 0xFF);
        }
        return frame;
    }

    private byte[] roundtrip(final int width, final int height, final byte[] frame) {
        final byte[] encoded = encoder.encode(frame, width, height);
        return decoder.decode(encoded, width, height).orElseThrow();
    }

    @Test
    void keyframeRoundtripIsBitExact() {
        final byte[] frame = frame(640, 480, i -> (i * 31) & 0xFFFF);
        assertArrayEquals(frame, roundtrip(640, 480, frame));
    }

    @Test
    void solidFrameEncodesTiny() {
        final byte[] frame = frame(640, 480, i -> 0xF800);
        final byte[] encoded = encoder.encode(frame, 640, 480);
        assertTrue(
                encoded.length < 4096,
                "solid frame should compress to a few KB, got " + encoded.length);
        assertArrayEquals(frame, decoder.decode(encoded, 640, 480).orElseThrow());
    }

    @Test
    void unchangedFrameProducesMinimalDelta() {
        final byte[] frame = frame(320, 240, i -> i & 0xFF);
        roundtrip(320, 240, frame);
        final byte[] delta = encoder.encode(frame, 320, 240);
        assertTrue(delta.length <= 8, "no dirty tiles: header only, got " + delta.length);
        assertArrayEquals(frame, decoder.decode(delta, 320, 240).orElseThrow());
    }

    @Test
    void partialChangeTransmitsOnlyDirtyTiles() {
        final int width = 640;
        final int height = 480;
        final byte[] first = frame(width, height, i -> 0x0000);
        roundtrip(width, height, first);

        final byte[] second = first.clone();
        // Change a handful of pixels in one corner tile.
        for (int i = 0; i < 10; i++) {
            second[i * 2] = (byte) 0xFF;
            second[i * 2 + 1] = 0x07;
        }
        final byte[] delta = encoder.encode(second, width, height);
        assertTrue(delta.length < 512, "single dirty tile should be tiny");
        assertArrayEquals(second, decoder.decode(delta, width, height).orElseThrow());
    }

    @Test
    void noisyFrameWorstCaseNearRawSize() {
        final int width = 256;
        final int height = 128;
        final Random random = new Random(7);
        final byte[] frame = new byte[width * height * 2];
        random.nextBytes(frame);

        roundtrip(width, height, frame);

        // Second encode of the identical noise: no dirty tiles at all.
        final byte[] delta = encoder.encode(frame, width, height);
        assertTrue(delta.length <= 8, "identical noise must yield an empty delta");

        // Fresh encoder: worst case is a keyframe of incompressible data.
        final byte[] encoded = new DeltaFrameCodec().encode(frame, width, height);
        assertTrue(encoded.length <= frame.length + 256, "worst case must not explode");
        assertArrayEquals(frame, decoder.decode(encoded, width, height).orElseThrow());
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    void nonTileAlignedSizesRoundtrip() {
        for (final int[] size : new int[][] {{100, 50}, {33, 17}, {31, 15}, {32, 16}, {65, 33}}) {
            final int width = size[0];
            final int height = size[1];
            final DeltaFrameCodec enc = new DeltaFrameCodec();
            final DeltaFrameCodec dec = new DeltaFrameCodec();
            byte[] current = frame(width, height, i -> (i * 7) ^ (i << 3));
            for (int step = 0; step < 3; step++) {
                current = current.clone();
                final Random random = new Random(step);
                for (int k = 0; k < 50; k++) {
                    final int index = random.nextInt(current.length / 2);
                    final short value = (short) random.nextInt(0x10000);
                    current[index * 2] = (byte) (value & 0xFF);
                    current[index * 2 + 1] = (byte) ((value >>> 8) & 0xFF);
                }
                final byte[] encoded = enc.encode(current, width, height);
                assertArrayEquals(
                        current, dec.decode(encoded, width, height).orElseThrow(),
                        "size " + width + "x" + height + " step " + step);
            }
        }
    }

    @Test
    void resolutionChangeProducesKeyframeAndResyncs() {
        final byte[] small = frame(320, 240, i -> 0x001F);
        roundtrip(320, 240, small);

        final byte[] big = frame(640, 480, i -> (0xF800 >> 3) & 0xFFFF);
        final byte[] encodedBig = encoder.encode(big, 640, 480);
        assertNotEquals(0, encodedBig[0] & Flag.KEYFRAME);
        assertArrayEquals(big, decoder.decode(encodedBig, 640, 480).orElseThrow());

        // A 320x240 delta stream (from an independent encoder) must be rejected
        // cleanly by a decoder now expecting 640x480.
        final DeltaFrameCodec otherEncoder = new DeltaFrameCodec();
        otherEncoder.encode(frame(320, 240, i -> 0x0000), 320, 240);
        final byte[] staleDelta =
                otherEncoder.encode(frame(320, 240, i -> 0x0011), 320, 240);
        assertEquals(0, staleDelta[0] & Flag.KEYFRAME);
        assertTrue(decoder.decode(staleDelta, 640, 480).isEmpty());
    }

    private static final class Flag {
        static final int KEYFRAME = 1;
    }
}
