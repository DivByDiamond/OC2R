package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** What a watcher decodes must be the picture the guest drew, for every codec and across codec switches. */
class FrameCodecRoundTripTest {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 200;

    @Test
    void everyCodecCarriesTheTwoToneCheckerboard() {
        for (final VideoCodec codec : VideoCodec.values()) {
            final FrameCodec server = new FrameCodec();
            final FrameCodec client = new FrameCodec();
            final byte[] decoded = roundTrip(server, client, codec, 0x07E0, 0x001F);
            assertClose(codec, decoded, 0x07E0, 0x001F);
        }
    }

    @Test
    void switchingCodecsMidStreamStillDeliversTheNewPicture() {
        final FrameCodec server = new FrameCodec();
        final FrameCodec client = new FrameCodec();
        final VideoCodec[] order = {VideoCodec.RAW, VideoCodec.H264, VideoCodec.DELTA, VideoCodec.H264, VideoCodec.RAW};
        final int[][] colors = {{0x07E0, 0x001F}, {0xFFE0, 0x07FF}, {0xF81F, 0x0000}, {0x07E0, 0xF800}, {0xFFFF, 0x0000}};
        for (int i = 0; i < order.length; i++) {
            final byte[] decoded = roundTrip(server, client, order[i], colors[i][0], colors[i][1]);
            assertClose(order[i], decoded, colors[i][0], colors[i][1]);
        }
    }

    private static byte[] roundTrip(final FrameCodec server, final FrameCodec client, final VideoCodec codec,
                                    final int a, final int b) {
        final byte[] frame = new byte[WIDTH * HEIGHT * 2];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                final int pixel = ((x / 8 + y / 8) & 1) == 0 ? a : b;
                frame[(y * WIDTH + x) * 2] = (byte) pixel;
                frame[(y * WIDTH + x) * 2 + 1] = (byte) (pixel >>> 8);
            }
        }
        final FrameCodec.EncodedFrame encoded = server.encode(codec, frame, WIDTH, HEIGHT);
        final var decoded = client.decode(encoded.codec(), encoded.data(), WIDTH, HEIGHT);
        assertTrue(decoded.isPresent(), codec + " frame did not decode");
        return decoded.get();
    }

    private static void assertClose(final VideoCodec codec, final byte[] frame, final int a, final int b) {
        assertClose(codec, frame, 12, 12, a);
        assertClose(codec, frame, 12, 148, b);
    }

    private static void assertClose(final VideoCodec codec, final byte[] frame, final int x, final int y, final int want) {
        final int pixel = (frame[(y * WIDTH + x) * 2] & 0xFF) | (frame[(y * WIDTH + x) * 2 + 1] & 0xFF) << 8;
        final int[] got = {(pixel >> 11) & 0x1F, (pixel >> 5) & 0x3F, pixel & 0x1F};
        final int[] expected = {(want >> 11) & 0x1F, (want >> 5) & 0x3F, want & 0x1F};
        final int[] tolerance = {5, 10, 5};
        for (int c = 0; c < 3; c++) {
            assertEquals(expected[c], got[c], tolerance[c], codec + " channel " + c + " at " + x + "," + y);
        }
    }
}
