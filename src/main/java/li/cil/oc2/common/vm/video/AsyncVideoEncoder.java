package li.cil.oc2.common.vm.video;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Moves the video encode path (RGB565 to YUV420 conversion plus codec work) off the
 * server thread onto a single shared daemon worker.
 *
 * <p>Lifecycle of a frame:
 * <ol>
 *   <li>the caller (server thread) obtains a buffer via {@link #obtainBuffer}, fills it
 *       with the framebuffer contents and submits it via {@link #offer};</li>
 *   <li>the inbox holds at most one pending job — submitting a new job evicts an older
 *       one that has not been picked up yet (last-frame-wins), which keeps slow encoders
 *       from falling behind without ever blocking the server thread;</li>
 *   <li>the worker encodes the frame and publishes the result to an outbox;</li>
 *   <li>the caller drains the outbox on a later tick via {@link #poll} and returns the
 *       source buffer to the pool via {@link #recycle} once it is done slicing it.</li>
 * </ol>
 *
 * <p>Buffer ownership is exclusive at any point in time: a buffer is either held by the
 * producer, sitting in the inbox, owned by the worker while encoding, waiting in the
 * outbox or back in the pool. Dropping frames (full inbox/outbox) always recycles the
 * dropped buffer. Because {@code FrameCodec.encode} may return its input array as-is
 * (RAW passthrough and overflow fallback), a completed frame's data must only be read
 * up to {@link #recycle}; network chunks are copied by {@code FrameChunker.slice}.
 *
 * <p>All jobs share one worker but carry their own {@link FrameCodec} instance, so codec
 * state stays thread-confined: each instance is only ever touched by the server thread
 * (never) or the worker (exactly one), never both.
 */
public final class AsyncVideoEncoder {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int INBOX_CAPACITY = 1;
    private static final int OUTBOX_CAPACITY = 8;
    private static final long IDLE_POLL_MS = 500;

    /** A raw RGB565 frame waiting to be encoded. */
    @SuppressWarnings("ArrayRecordComponent")
    public record PendingFrame(FrameCodec codec, VideoCodec codecType, byte[] rgb565, int width, int height) {}

    /** An encoded frame ready to be sent, plus the source buffer for pool recycling. */
    @SuppressWarnings("ArrayRecordComponent")
    public record CompletedFrame(FrameCodec.EncodedFrame frame, byte[] buffer, int width, int height) {}

    // Static queues so every instance shares a single daemon worker regardless of how
    // many monitors/projectors exist; jobs are self-contained so instances cannot
    // interfere beyond competing for encode time.
    private static final ArrayBlockingQueue<PendingFrame> INBOX = new ArrayBlockingQueue<>(INBOX_CAPACITY);
    private static final ArrayBlockingQueue<CompletedFrame> OUTBOX = new ArrayBlockingQueue<>(OUTBOX_CAPACITY);
    private static final ConcurrentLinkedQueue<byte[]> BUFFER_POOL = new ConcurrentLinkedQueue<>();

    @Nullable private static Thread worker;

    /**
     * Returns a pooled buffer of exactly {@code length} bytes for framebuffer copying.
     */
    public byte[] obtainBuffer(final int length) {
        final List<byte[]> mismatched = new ArrayList<>();
        byte[] match = null;
        for (;;) {
            final byte[] candidate = BUFFER_POOL.poll();
            if (candidate == null) {
                break;
            }
            if (match == null && candidate.length == length) {
                match = candidate;
            } else {
                mismatched.add(candidate);
            }
        }
        BUFFER_POOL.addAll(mismatched);
        return match != null ? match : new byte[length];
    }

    /**
     * Submits a filled buffer for encoding on the worker thread, replacing any older
     * not-yet-claimed frame (last-frame-wins). Never blocks; the oldest unclaimed job
     * is silently dropped if the worker lags behind, which is safe because dirty flags
     * of the device were already consumed and any further change produces a new frame.
     */
    public void offer(final FrameCodec codec, final VideoCodec codecType, final byte[] rgb565, final int width, final int height) {
        final PendingFrame evicted = INBOX.poll();
        if (evicted != null) {
            recycle(evicted.rgb565());
        }
        INBOX.offer(new PendingFrame(codec, codecType, rgb565, width, height));
        ensureWorker();
    }

    /**
     * Takes the oldest encoded frame from the outbox, if any. Called on the server thread.
     */
    @Nullable
    public CompletedFrame poll() {
        return OUTBOX.poll();
    }

    /**
     * Returns a drained source buffer to the pool for reuse.
     */
    public void recycle(final byte[] buffer) {
        BUFFER_POOL.add(buffer);
    }

    private static void ensureWorker() {
        if (worker != null) return;
        startWorker();
    }

    private static synchronized void startWorker() {
        if (worker != null) return;
        final Thread thread = new Thread(AsyncVideoEncoder::runWorker, "OC2R Video Encoder");
        thread.setDaemon(true);
        thread.start();
        worker = thread;
    }

    private static void runWorker() {
        while (true) {
            final PendingFrame job;
            try {
                job = INBOX.poll(IDLE_POLL_MS, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException e) {
                return;
            }
            if (job == null) continue;
            encodeJob(job);
        }
    }

    private static void encodeJob(final PendingFrame job) {
        FrameCodec.EncodedFrame result = null;
        try {
            result = job.codec().encode(job.codecType(), job.rgb565(), job.width(), job.height());
        } catch (final Exception e) {
            LOGGER.warn("Video frame encoding failed", e);
        }
        if (result == null || result.data().length == 0) {
            BUFFER_POOL.add(job.rgb565());
            return;
        }

        final CompletedFrame completed =
                new CompletedFrame(result, job.rgb565(), job.width(), job.height());
        publish(completed);
    }

    private static void publish(final CompletedFrame completed) {
        while (!OUTBOX.offer(completed)) {
            final CompletedFrame dropped = OUTBOX.poll();
            if (dropped == null) continue;
            BUFFER_POOL.add(dropped.buffer());
        }
    }
}
