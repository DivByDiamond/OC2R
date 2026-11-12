package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

/**
 * Measures what each video codec puts on the wire for a desktop-like picture: one full frame, then a
 * stream in which only a small clock area changes. The encoded size is exactly what the monitor sends
 * to its watchers, so the figures are the codec's share of the traffic.
 */
@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // each simulated frame is a fresh buffer
class VideoCodecTrafficTest {
    private static final int UPDATES = 30;

    private record Result(long firstFrame, long updates) {}

    @Test
    void h264ShrinksADesktopFrameAndDeltaShrinksSmallUpdates() {
        final StringBuilder table = new StringBuilder("\nvideo codec traffic (bytes)\n");
        for (final int[] size : new int[][] {{320, 200}, {640, 400}, {1024, 768}}) {
            final int width = size[0];
            final int height = size[1];
            final long rawFrame = (long) width * height * 2;
            final Result raw = stream(VideoCodec.RAW, width, height);
            final Result h264 = stream(VideoCodec.H264, width, height);
            final Result delta = stream(VideoCodec.DELTA, width, height);
            table.append(String.format(Locale.ROOT, "%dx%d raw frame=%d%n", width, height, rawFrame));
            for (final var entry : new Object[][] {{"RAW", raw}, {"H264", h264}, {"DELTA", delta}}) {
                final Result result = (Result) entry[1];
                table.append(String.format(Locale.ROOT, "  %-5s first=%8d  per update=%8d  (%.1fx smaller first, %.1fx per update)%n",
                        entry[0], result.firstFrame, result.updates / UPDATES,
                        (double) rawFrame / result.firstFrame, (double) rawFrame * UPDATES / result.updates));
            }
            assertTrue(h264.firstFrame * 3 < rawFrame, width + "x" + height + ": H264 first frame is not several times smaller");
            assertTrue(h264.updates / UPDATES * 3 < rawFrame, width + "x" + height + ": H264 updates are not several times smaller");
            assertTrue(delta.updates / UPDATES * 20 < rawFrame, width + "x" + height + ": DELTA updates are not far smaller than raw");
        }
        LogManager.getLogger().info(table);
    }

    private static Result stream(final VideoCodec codec, final int width, final int height) {
        final FrameCodec encoder = new FrameCodec();
        final byte[] frame = desktop(width, height);
        final long first = encoder.encode(codec, frame.clone(), width, height).data().length;
        long updates = 0;
        long state = 12345;
        for (int i = 0; i < UPDATES; i++) {
            // The clock: a 72x14 patch near the top right changes every frame.
            for (int y = 4; y < 18; y++) {
                for (int x = width - 80; x < width - 8; x++) {
                    state = state * 6364136223846793005L + 1442695040888963407L;
                    final int pixel = (state >>> 33) % 5 == 0 ? 0xFFFF : 0x2104;
                    frame[(y * width + x) * 2] = (byte) pixel;
                    frame[(y * width + x) * 2 + 1] = (byte) (pixel >>> 8);
                }
            }
            updates += encoder.encode(codec, frame.clone(), width, height).data().length;
        }
        return new Result(first, updates);
    }

    /** Title bar, two windows with text-like lines, a dock; the content that usually fills an OC2R screen. */
    private static byte[] desktop(final int width, final int height) {
        final byte[] frame = new byte[width * height * 2];
        fill(frame, width, 0, 0, width, height, 0x2104);
        fill(frame, width, 0, 0, width, 20, 0x4A69);
        fill(frame, width, 0, height - 24, width, 24, 0x4A69);
        final int[] windows = {width / 16, height / 8, width * 3 / 8, height * 5 / 8,
                width / 2, height / 8, width * 7 / 16, height * 5 / 8};
        for (int w = 0; w < windows.length; w += 4) {
            fill(frame, width, windows[w], windows[w + 1], windows[w + 2], windows[w + 3], 0xF79E);
            fill(frame, width, windows[w], windows[w + 1], windows[w + 2], 16, 0x07FF);
            long state = 99 + w;
            for (int line = 0; line < windows[w + 3] / 12 - 2; line++) {
                int x = windows[w] + 6;
                final int y = windows[w + 1] + 24 + line * 12;
                while (x < windows[w] + windows[w + 2] - 12) {
                    state = state * 6364136223846793005L + 1442695040888963407L;
                    final int glyph = 3 + (int) ((state >>> 40) % 5);
                    if ((state >>> 33) % 6 != 0) fill(frame, width, x, y, glyph, 8, 0x2104);
                    x += glyph + 2;
                }
            }
        }
        return Arrays.copyOf(frame, frame.length);
    }

    private static void fill(final byte[] frame, final int width, final int x0, final int y0,
                             final int w, final int h, final int pixel) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                final int index = (y * width + x) * 2;
                if (index + 1 < frame.length) {
                    frame[index] = (byte) pixel;
                    frame[index + 1] = (byte) (pixel >>> 8);
                }
            }
        }
    }
}
