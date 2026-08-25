package li.cil.oc2.common.network.util.frame;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;

public final class FrameChunker {
    public static final int MAX_CHUNK_SIZE = 256 * 1024;
    /** Upper bound for a reassembled frame; largest real frame is 4K x 2K x 2 bytes. */
    public static final int MAX_FRAME_SIZE = 32 * 1024 * 1024;
    private static final long PARTIAL_TIMEOUT_MS = 5_000;

    private FrameChunker() {}

    public static int chunkCount(final int frameSize) {
        return Math.max(1, (frameSize + MAX_CHUNK_SIZE - 1) / MAX_CHUNK_SIZE);
    }

    public static byte[] slice(final byte[] frame, final int index) {
        final int from = index * MAX_CHUNK_SIZE;
        final int to = Math.min(frame.length, from + MAX_CHUNK_SIZE);
        final int size = to - from;
        final byte[] chunk = new byte[size];
        System.arraycopy(frame, from, chunk, 0, size);
        return chunk;
    }

    /**
     * Reassembles chunked frames arriving over the network, keyed by block position.
     *
     * <p>Every chunk is validated (dimensions, frame size cap, chunk count derived
     * from frame size, chunk index bounds, exact expected payload length) before it
     * is copied into the partial buffer, so malformed traffic can never corrupt
     * state beyond discarding the offending chunk. A partial whose parameters do
     * not match an incoming chunk is silently replaced with a fresh one.
     */
    public static final class Reassembler {
        public record CompletedFrame(int codec, int width, int height, byte[] data) {}

        private static final class Partial {
            final int codec;
            final int width;
            final int height;
            final int chunkCount;
            final byte[] data;
            final BitSet received;
            final long createdAt;

            Partial(
                    final int codec,
                    final int width,
                    final int height,
                    final int frameSize,
                    final int chunkCount) {
                this.codec = codec;
                this.width = width;
                this.height = height;
                this.chunkCount = chunkCount;
                this.data = new byte[frameSize];
                this.received = new BitSet(chunkCount);
                this.createdAt = System.currentTimeMillis();
            }
        }

        @SuppressWarnings("PMD.UseConcurrentHashMap")
        private final Map<BlockPos, Partial> partials = new HashMap<>();

        @Nullable
        public synchronized CompletedFrame offer(
                final BlockPos pos,
                final int codec,
                final int width,
                final int height,
                final int frameSize,
                final int chunkIndex,
                final int chunkCount,
                final byte[] data) {
            if (!isValid(codec, width, height, frameSize, chunkIndex, chunkCount)) {
                return null;
            }

            final Partial partial = acquirePartial(pos, codec, width, height, frameSize, chunkCount);

            if (!partial.received.get(chunkIndex)) {
                final int from = chunkIndex * MAX_CHUNK_SIZE;
                // Chunk payloads come from the network; anything but the exact
                // expected size would corrupt the frame or throw here.
                final int expectedSize = Math.min(MAX_CHUNK_SIZE, frameSize - from);
                if (data.length != expectedSize) {
                    return null;
                }
                System.arraycopy(data, 0, partial.data, from, data.length);
                partial.received.set(chunkIndex);
            }

            evictExpired();

            if (partial.received.cardinality() < chunkCount) {
                return null;
            }

            partials.remove(pos);
            return new CompletedFrame(partial.codec, partial.width, partial.height, partial.data);
        }

        private boolean isValid(
                final int codec,
                final int width,
                final int height,
                final int frameSize,
                final int chunkIndex,
                final int chunkCount) {
            return codec >= 0
                    && width > 0
                    && height > 0
                    && frameSize > 0
                    && frameSize <= MAX_FRAME_SIZE
                    && chunkCount > 0
                    && chunkCount == FrameChunker.chunkCount(frameSize)
                    && chunkIndex >= 0
                    && chunkIndex < chunkCount;
        }

        private Partial acquirePartial(
                final BlockPos pos,
                final int codec,
                final int width,
                final int height,
                final int frameSize,
                final int chunkCount) {
            final Partial partial = partials.get(pos);
            if (partial == null
                    || partial.codec != codec
                    || partial.data.length != frameSize
                    || partial.chunkCount != chunkCount) {
                final Partial fresh = new Partial(codec, width, height, frameSize, chunkCount);
                partials.put(pos, fresh);
                return fresh;
            }
            return partial;
        }

        // Drops partials that have been incomplete for longer than
        // PARTIAL_TIMEOUT_MS, so abandoned transfers cannot accumulate forever.
        // Invoked from offer() because there is no tick or other hook to run
        // cleanup from; an active transfer resumed after a gap simply restarts.
        private void evictExpired() {
            final long now = System.currentTimeMillis();
            final Iterator<Partial> iterator = partials.values().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().createdAt > PARTIAL_TIMEOUT_MS) {
                    iterator.remove();
                }
            }
        }
    }
}
