package li.cil.oc2.common.network.message.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class MonitorStateMessageTest {
    @Test
    void preservesAllAuthoritativeStateCombinations() {
        final BlockPos position = new BlockPos(-12, 100, 34);
        for (int flags = 0; flags < 8; flags++) {
            final MonitorStateMessage message = new MonitorStateMessage( // NOPMD - Each test case needs independent fixture inputs.
                    position, (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0);
            assertEquals(message, roundtrip(message));
        }
    }

    @Test
    void powerOnlyTransitionsReachOtherClientsWhileMountedAndCharged() {
        final BlockPos position = new BlockPos(3, 100, 2);
        final MonitorStateMessage before = roundtrip(new MonitorStateMessage(position, true, true, false));
        final MonitorStateMessage started = roundtrip(new MonitorStateMessage(position, true, true, true));
        final MonitorStateMessage stopped = roundtrip(new MonitorStateMessage(position, true, true, false));

        assertFalse(before.isPowered());
        assertTrue(started.isMounted());
        assertTrue(started.hasEnergy());
        assertTrue(started.isPowered(), "a server power toggle needs no mount/energy change to synchronize");
        assertTrue(stopped.isMounted());
        assertTrue(stopped.hasEnergy());
        assertFalse(stopped.isPowered());
    }

    private static MonitorStateMessage roundtrip(final MonitorStateMessage message) {
        final ByteBuf buffer = Unpooled.buffer();
        try {
            MonitorStateMessage.STREAM_CODEC.encode(buffer, message);
            final MonitorStateMessage result = MonitorStateMessage.STREAM_CODEC.decode(buffer);
            assertFalse(buffer.isReadable(), "the decoder must consume all state fields");
            return result;
        } finally {
            buffer.release();
        }
    }
}
