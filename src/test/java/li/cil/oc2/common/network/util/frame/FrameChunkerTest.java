package li.cil.oc2.common.network.util.frame;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Random;
import li.cil.oc2.common.vm.video.VideoCodec;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class FrameChunkerTest {
    private static final int CODEC = VideoCodec.RAW.id;

    @Test
    void roundTripSingleChunk() {
        final byte[] frame = new byte[1024];
        new Random(1).nextBytes(frame);
        final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
        final FrameChunker.Reassembler.CompletedFrame completed =
                reassembler.offer(
                        new BlockPos(0, 0, 0), CODEC, 32, 16, frame.length, 0, 1,
                        FrameChunker.slice(frame, 0));
        assertEquals(CODEC, completed.codec());
        assertEquals(32, completed.width());
        assertEquals(16, completed.height());
        assertArrayEquals(frame, completed.data());
    }

    @Test
    void roundTripMultipleChunks() {
        final byte[] frame = new byte[640 * 480 * 2];
        new Random(1).nextBytes(frame);
        final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
        final BlockPos pos = new BlockPos(10, 20, 30);
        final int count = FrameChunker.chunkCount(frame.length);
        assertEquals(3, count);
        for (int i = 0; i < count; i++) {
            final FrameChunker.Reassembler.CompletedFrame completed =
                    reassembler.offer(
                            pos, CODEC, 640, 480, frame.length, i, count,
                            FrameChunker.slice(frame, i));
            if (i < count - 1) {
                assertNull(completed);
            } else {
                assertEquals(640, completed.width());
                assertEquals(480, completed.height());
                assertArrayEquals(frame, completed.data());
            }
        }
    }

    @Test
    void chunkSizeBoundary() {
        final byte[] frame = new byte[FrameChunker.MAX_CHUNK_SIZE];
        new Random(2).nextBytes(frame);
        final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
        final BlockPos pos = new BlockPos(0, 0, 0);
        assertEquals(1, FrameChunker.chunkCount(frame.length));
        final FrameChunker.Reassembler.CompletedFrame completed =
                reassembler.offer(
                        pos, CODEC, 512, 256, frame.length, 0, 1,
                        FrameChunker.slice(frame, 0));
        assertArrayEquals(frame, completed.data());
    }

    @Test
    void variableSizeFrame() {
        final byte[] frame = new byte[4096];
        new Random(3).nextBytes(frame);
        final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
        final BlockPos pos = new BlockPos(5, 5, 5);
        final int count = FrameChunker.chunkCount(frame.length);
        for (int i = 0; i < count; i++) {
            final FrameChunker.Reassembler.CompletedFrame completed =
                    reassembler.offer(
                            pos, CODEC, 640, 480, frame.length, i, count,
                            FrameChunker.slice(frame, i));
            if (i == count - 1) {
                assertEquals(frame.length, completed.data().length);
                assertArrayEquals(frame, completed.data());
            }
        }
    }

    @Test
    void negativeChunkIndexIsRejected() {
        final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
        assertNull(
                reassembler.offer(
                        new BlockPos(0, 0, 0), CODEC, 32, 16, 1024, -1, 1,
                        new byte[1024]));
    }

    @Test
    void wrongChunkCountIsRejected() {
        final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
        assertNull(
                reassembler.offer(
                        new BlockPos(0, 0, 0), CODEC, 32, 16, 1024, 0, 2,
                        new byte[1024]));
    }

    @Test
    void wrongChunkPayloadSizeIsRejected() {
        final byte[] frame = new byte[640 * 480 * 2];
        final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
        final BlockPos pos = new BlockPos(1, 2, 3);
        final int count = FrameChunker.chunkCount(frame.length);
        assertNull(
                reassembler.offer(
                        pos, CODEC, 640, 480, frame.length, 0, count,
                        new byte[FrameChunker.MAX_CHUNK_SIZE + 1]));
        assertNull(
                reassembler.offer(
                        pos, CODEC, 640, 480, frame.length, 0, count,
                        new byte[FrameChunker.MAX_CHUNK_SIZE - 1]));
    }

    @Test
    void oversizedFrameIsRejected() {
        final FrameChunker.Reassembler reassembler = new FrameChunker.Reassembler();
        assertNull(
                reassembler.offer(
                        new BlockPos(0, 0, 0), CODEC, 32, 16, Integer.MAX_VALUE, 0,
                        FrameChunker.chunkCount(Integer.MAX_VALUE),
                        new byte[FrameChunker.MAX_CHUNK_SIZE]));
    }
}
