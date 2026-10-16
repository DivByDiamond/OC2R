package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void unrelatedDeviceCannotConsumeCompletedFrame() throws Exception {
        final AsyncVideoEncoder unrelated = new AsyncVideoEncoder();
        final byte[] frame = new byte[16 * 16 * 2];
        encoder.offer(codec, VideoCodec.RAW, frame, 16, 16);
        final long deadline = System.currentTimeMillis() + 10_000;
        AsyncVideoEncoder.CompletedFrame completed = null;
        while (completed == null && System.currentTimeMillis() < deadline) {
            assertNull(unrelated.poll(), "A different display stole this stream's completion");
            completed = encoder.poll();
            Thread.sleep(1);
        }
        assertNotNull(completed);
        assertSame(frame, completed.buffer());
    }

    @Test
    void pendingFramesOfDifferentDevicesCannotEvictEachOther() throws Exception {
        final GatedCodec gate = new GatedCodec();
        encoder.offer(gate, VideoCodec.RAW, new byte[512], 16, 16);
        assertTrue(gate.entered.await(10, TimeUnit.SECONDS));
        final AsyncVideoEncoder second = new AsyncVideoEncoder();
        final AsyncVideoEncoder third = new AsyncVideoEncoder();
        final byte[] secondFrame = new byte[512];
        final byte[] thirdFrame = new byte[512];
        try {
            second.offer(new FrameCodec(), VideoCodec.RAW, secondFrame, 16, 16);
            third.offer(new FrameCodec(), VideoCodec.RAW, thirdFrame, 16, 16);
        } finally {
            gate.release.countDown();
        }
        assertNotNull(pollUntilCompleted(encoder));
        assertSame(secondFrame, pollUntilCompleted(second).buffer());
        assertSame(thirdFrame, pollUntilCompleted(third).buffer());
    }

    @Test
    void fullOutboxKeepsDeltaHistoryAndOnlyReplacesUnencodedInput() throws Exception {
        final CountingCodec counting = new CountingCodec();
        final FrameCodec decoder = new FrameCodec();
        for (int index = 0; index < 8; index++) {
            encoder.offer(counting, VideoCodec.DELTA, solidFrame(index), 16, 16);
            awaitEncoded(counting, index + 1);
        }
        encoder.offer(counting, VideoCodec.DELTA, solidFrame(8), 16, 16);
        encoder.offer(counting, VideoCodec.DELTA, solidFrame(9), 16, 16);
        Thread.sleep(100);
        assertEquals(8, counting.encoded.get(), "Full outbox must not advance the delta baseline");
        for (int index = 0; index < 8; index++) {
            final var completed = pollUntilCompleted(encoder);
            assertArrayEquals(solidFrame(index), decoder.decode(VideoCodec.DELTA,
                    completed.frame().data(), 16, 16).orElseThrow());
        }
        final var latest = pollUntilCompleted(encoder);
        assertArrayEquals(solidFrame(9), decoder.decode(VideoCodec.DELTA,
                latest.frame().data(), 16, 16).orElseThrow());
        assertEquals(9, counting.encoded.get());
    }

    private static byte[] solidFrame(final int value) {
        final byte[] frame = new byte[512];
        Arrays.fill(frame, (byte) value);
        return frame;
    }

    private static void awaitEncoded(final CountingCodec codec, final int expected) throws Exception {
        final long deadline = System.currentTimeMillis() + 10_000;
        while (codec.encoded.get() < expected && System.currentTimeMillis() < deadline) Thread.sleep(1);
        assertEquals(expected, codec.encoded.get());
    }

    private static final class CountingCodec extends FrameCodec {
        final AtomicInteger encoded = new AtomicInteger();

        @Override
        public EncodedFrame encode(final VideoCodec type, final byte[] frame, final int width, final int height) {
            final EncodedFrame result = super.encode(type, frame, width, height);
            encoded.incrementAndGet();
            return result;
        }
    }

    private AsyncVideoEncoder.CompletedFrame pollUntilCompleted() throws Exception {
        return pollUntilCompleted(encoder);
    }

    private static AsyncVideoEncoder.CompletedFrame pollUntilCompleted(final AsyncVideoEncoder encoder) throws Exception {
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
