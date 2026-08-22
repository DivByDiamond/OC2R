package li.cil.oc2.common.network.util.frame;

import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;

public final class FrameChunker {
    public static final int MAX_CHUNK_SIZE = 256 * 1024;
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

    public static final class Reassembler {
        public record CompletedFrame(int width, int height, byte[] data) {}

        private static final class Partial {
            final int width;
            final int height;
            final int chunkCount;
            final byte[] data;
            final BitSet received;
            final long createdAt;

            Partial(final int width, final int height, final int chunkCount) {
                this.width = width;
                this.height = height;
                this.chunkCount = chunkCount;
                this.data = new byte[width * height * 2];
                this.received = new BitSet(chunkCount);
                this.createdAt = System.currentTimeMillis();
            }
        }

        private final Map<BlockPos, Partial> partials = new HashMap<>();

        @Nullable
        public synchronized CompletedFrame offer(
                final BlockPos pos,
                final int width,
                final int height,
                final int chunkIndex,
                final int chunkCount,
                final byte[] data) {
            if (width <= 0 || height <= 0 || chunkCount <= 0 || chunkIndex >= chunkCount) {
                return null;
            }
            final int expectedSize = width * height * 2;

            Partial partial = partials.get(pos);
            if (partial == null
                    || partial.data.length != expectedSize
                    || partial.chunkCount != chunkCount) {
                partial = new Partial(width, height, chunkCount);
                partials.put(pos, partial);
            }

            if (!partial.received.get(chunkIndex)) {
                final int from = chunkIndex * MAX_CHUNK_SIZE;
                System.arraycopy(data, 0, partial.data, from, data.length);
                partial.received.set(chunkIndex);
            }

            evictExpired();

            if (partial.received.cardinality() < chunkCount) {
                return null;
            }

            partials.remove(pos);
            return new CompletedFrame(width, height, partial.data);
        }

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
