package li.cil.oc2.client.audio;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PcmSoundBuffer {
    private static final int MAX_CHUNK_SIZE = 4096;

    private final Queue<byte[]> chunks = new ConcurrentLinkedQueue<>();
    private volatile long lastWriteTime;
    private volatile long totalBytes;

    private byte[] currentChunk;
    private int currentChunkOffset;

    public void write(final byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        for (int offset = 0; offset < data.length; offset += MAX_CHUNK_SIZE) {
            final int length = Math.min(MAX_CHUNK_SIZE, data.length - offset);
            chunks.add(Arrays.copyOfRange(data, offset, offset + length));
        }
        lastWriteTime = System.currentTimeMillis();
        totalBytes += data.length;
    }

    public int read(final byte[] out) {
        int written = 0;
        while (written < out.length) {
            if (currentChunk != null && currentChunkOffset >= currentChunk.length) {
                currentChunk = null;
                currentChunkOffset = 0;
            }
            if (currentChunk == null) {
                currentChunk = chunks.poll();
                currentChunkOffset = 0;
                if (currentChunk == null) {
                    break;
                }
            }
            final int toCopy =
                    Math.min(currentChunk.length - currentChunkOffset, out.length - written);
            System.arraycopy(currentChunk, currentChunkOffset, out, written, toCopy);
            currentChunkOffset += toCopy;
            written += toCopy;
        }
        return written;
    }

    public boolean isStale(final long timeoutMs) {
        return currentChunk == null
                && chunks.isEmpty()
                && System.currentTimeMillis() - lastWriteTime > timeoutMs;
    }

    public int size() {
        return (int) Math.min(totalBytes, Integer.MAX_VALUE);
    }
}