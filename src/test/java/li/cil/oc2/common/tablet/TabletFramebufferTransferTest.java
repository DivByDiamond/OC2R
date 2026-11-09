package li.cil.oc2.common.tablet;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import li.cil.oc2.common.network.message.tablet.TabletFramebufferMessage;
import net.minecraft.network.FriendlyByteBuf;
import li.cil.oc2.common.tablet.display.TabletDisplayDevice;
import li.cil.oc2.common.vm.video.DeltaFrameCodec;
import org.junit.jupiter.api.Test;

class TabletFramebufferTransferTest {
    @Test void randomKeyframeAndChangedTilesReachTheClientWithoutLoss() {
        final UUID id = UUID.randomUUID();
        final TabletClientView view = TabletClientView.acquire(id);
        try {
            final byte[] pixels = new byte[TabletDisplayDevice.FRAME_BYTES];
            new Random(42).nextBytes(pixels);
            final DeltaFrameCodec encoder = new DeltaFrameCodec();
            deliver(view, encoder.encode(pixels, 256, 160));
            assertArrayEquals(pixels, view.framebuffer());
            Arrays.fill(pixels, 1000, 1200, (byte) 10);
            deliver(view, encoder.encode(pixels, 256, 160));
            assertArrayEquals(pixels, view.framebuffer());
            assertEquals(2, view.frameVersion());
        } finally { TabletClientView.release(id); }
    }

    @Test void malformedSizeAndOutOfOrderChunksCannotReplaceTheLastGoodFrame() {
        final UUID id = UUID.randomUUID();
        final TabletClientView view = TabletClientView.acquire(id);
        try {
            view.acceptFrame(Integer.MAX_VALUE, 0, 1, new byte[0]);
            view.acceptFrame(1, 0, 1, new byte[0]);
            view.acceptFrame(30_000, 1, 2, new byte[5_424]);
            assertEquals(0, view.frameVersion());
        } finally { TabletClientView.release(id); }
    }

    @Test void oversizedWireChunksAreRejectedBeforeAllocatingFrameStorage() {
        final UUID id = UUID.randomUUID();
        final byte[] oversized = new byte[TabletFramebufferMessage.MAX_CHUNK_SIZE + 1];
        assertThrows(EncoderException.class, () -> TabletFramebufferMessage.STREAM_CODEC.encode(
                Unpooled.buffer(), new TabletFramebufferMessage(id, oversized.length, 0, 1, oversized)));
        final FriendlyByteBuf forged = new FriendlyByteBuf(Unpooled.buffer());
        forged.writeUUID(id);
        forged.writeInt(oversized.length).writeInt(0).writeInt(1);
        forged.writeByteArray(oversized);
        assertThrows(DecoderException.class, () -> TabletFramebufferMessage.STREAM_CODEC.decode(forged));
    }

    private static void deliver(final TabletClientView view, final byte[] encoded) {
        final int chunk = TabletFramebufferMessage.MAX_CHUNK_SIZE;
        final int count = (encoded.length + chunk - 1) / chunk;
        for (int index = 0; index < count; index++) view.acceptFrame(encoded.length, index, count,
                Arrays.copyOfRange(encoded, index * chunk, Math.min(encoded.length, (index + 1) * chunk)));
    }
}
