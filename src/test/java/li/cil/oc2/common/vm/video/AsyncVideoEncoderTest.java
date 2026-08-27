package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class AsyncVideoEncoderTest {
    private final AsyncVideoEncoder encoder = new AsyncVideoEncoder();
    private final FrameCodec codec = new FrameCodec();

    @Test
    void rawFrameRoundtripsThroughWorker() throws Exception {
        final byte[] frame = new byte[64 * 32 * 2];
        new Random(1).nextBytes(frame);
        encoder.offer(codec, VideoCodec.RAW, frame, 64, 32);

        final AsyncVideoEncoder.CompletedFrame completed = pollUntilCompleted();
        assertEquals(VideoCodec.RAW, completed.frame().codec());
        assertEquals(64, completed.width());
        assertEquals(32, completed.height());
        // RAW passthrough returns the input array as-is; the caller must recycle it.
        assertSame(frame, completed.frame().data());
        assertSame(frame, completed.buffer());
    }

    @Test
    void lastSubmittedFrameWins() throws Exception {
        // Occupy the worker inside encode so it cannot claim the frames offered below.
        // This makes the eviction of the older unclaimed frame deterministic instead
        // of racing with the (very fast) worker startup.
        final GatedCodec gated = new GatedCodec();
        final byte[] gateBuffer = new byte[16 * 16 * 2];
        encoder.offer(gated, VideoCodec.RAW, gateBuffer, 16, 16);
        assertTrue(gated.entered.await(10, TimeUnit.SECONDS), "worker never entered the gated encode");

        final byte[] first = new byte[16 * 16 * 2];
        final byte[] second = new byte[16 * 16 * 2];
        second[0] = 1;
        encoder.offer(gated, VideoCodec.RAW, first, 16, 16);
        encoder.offer(gated, VideoCodec.RAW, second, 16, 16);

        gated.release.countDown();

        // The gate job publishes first; drain it, then the newest frame must be next.
        final AsyncVideoEncoder.CompletedFrame gateFrame = pollUntilCompleted();
        assertSame(gateBuffer, gateFrame.frame().data());

        final AsyncVideoEncoder.CompletedFrame completed = pollUntilCompleted();
        assertSame(second, completed.frame().data(), "the newer unclaimed frame should replace the older one");
    }

    @Test
    void recycledBufferIsReusedWhenSizeMatches() {
        final byte[] buffer = encoder.obtainBuffer(1024);
        assertEquals(1024, buffer.length);
        encoder.recycle(buffer);
        final byte[] reused = encoder.obtainBuffer(1024);
        assertSame(buffer, reused);
        final byte[] mismatched = encoder.obtainBuffer(2048);
        assertEquals(2048, mismatched.length);
    }

    @Test
    void outboxIsEmptyWithoutWork() {
        assertNull(encoder.poll());
    }

    private AsyncVideoEncoder.CompletedFrame pollUntilCompleted() throws Exception {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            final AsyncVideoEncoder.CompletedFrame completed = encoder.poll();
            if (completed != null) {
                encoder.recycle(completed.buffer());
                return completed;
            }
            Thread.sleep(10);
        }
        assertNotNull(null, "encoder did not complete a frame in time");
        throw new IllegalStateException();
    }

    /** A codec whose encode call blocks until the test releases it. */
    private static final class GatedCodec extends FrameCodec {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Override
        public FrameCodec.EncodedFrame encode(
                final VideoCodec codec, final byte[] rgb565, final int width, final int height) {
            entered.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("gated encode was never released");
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for the gate", e);
            }
            return super.encode(codec, rgb565, width, height);
        }
    }
}
