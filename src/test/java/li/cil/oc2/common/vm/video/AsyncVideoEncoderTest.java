package li.cil.oc2.common.vm.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Random;
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
        final byte[] first = new byte[16 * 16 * 2];
        final byte[] second = new byte[16 * 16 * 2];
        second[0] = 1;
        encoder.offer(codec, VideoCodec.RAW, first, 16, 16);
        encoder.offer(codec, VideoCodec.RAW, second, 16, 16);

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
}
