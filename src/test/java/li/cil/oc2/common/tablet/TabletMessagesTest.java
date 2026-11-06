package li.cil.oc2.common.tablet;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.nio.ByteBuffer;
import java.util.UUID;
import li.cil.oc2.common.network.message.tablet.TabletActionMessage;
import li.cil.oc2.common.network.message.tablet.TabletInputMessage;
import li.cil.oc2.common.vm.PowerAction;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TabletMessagesTest {
    @Test
    void inputRoundTripsWithItsTabletId() {
        final UUID id = UUID.randomUUID();
        final ByteBuf buffer = Unpooled.buffer();
        TabletInputMessage.STREAM_CODEC.encode(buffer, new TabletInputMessage(id, new byte[] {'l', 's', '\n'}));
        final TabletInputMessage decoded = TabletInputMessage.STREAM_CODEC.decode(buffer);
        assertEquals(id, decoded.tabletId());
        assertArrayEquals(new byte[] {'l', 's', '\n'}, decoded.data());
    }

    @Test
    void inputFromAByteBufferCopiesOnlyTheRemainingBytes() {
        final ByteBuffer source = ByteBuffer.wrap(new byte[] {1, 2, 3, 4});
        source.get();
        final TabletInputMessage message = new TabletInputMessage(UUID.randomUUID(), source);
        assertArrayEquals(new byte[] {2, 3, 4}, message.data());
        assertEquals(1, source.position(), "the caller's buffer is not consumed");
    }

    @Test
    void oversizedInputIsRejectedWhenEncodingAndWhenDecodingForgedPackets() {
        assertThrows(
                io.netty.handler.codec.EncoderException.class,
                () -> TabletInputMessage.STREAM_CODEC.encode(
                        Unpooled.buffer(),
                        new TabletInputMessage(UUID.randomUUID(), new byte[TabletInputMessage.MAX_BYTES + 1])));

        // A modified client can write any length; the receiver must refuse it before allocating.
        final FriendlyByteBuf forged = new FriendlyByteBuf(Unpooled.buffer());
        forged.writeUUID(UUID.randomUUID());
        forged.writeByteArray(new byte[TabletInputMessage.MAX_BYTES + 1]);
        assertThrows(DecoderException.class, () -> TabletInputMessage.STREAM_CODEC.decode(forged));
    }

    @Test
    void inputAtTheLimitIsAccepted() {
        final ByteBuf buffer = Unpooled.buffer();
        TabletInputMessage.STREAM_CODEC.encode(
                buffer, new TabletInputMessage(UUID.randomUUID(), new byte[TabletInputMessage.MAX_BYTES]));
        assertEquals(TabletInputMessage.MAX_BYTES, TabletInputMessage.STREAM_CODEC.decode(buffer).data().length);
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    void everyPowerActionMapsToTheActionOfTheSameName() {
        for (final PowerAction power : PowerAction.values()) {
            assertEquals(power.name(), TabletActionMessage.Action.of(power).name());
        }
    }

    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // One buffer and message per action.
    void actionsRoundTrip() {
        final UUID id = UUID.randomUUID();
        for (final TabletActionMessage.Action action : TabletActionMessage.Action.values()) {
            final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            TabletActionMessage.STREAM_CODEC.encode(buffer, new TabletActionMessage(id, action));
            final TabletActionMessage decoded = TabletActionMessage.STREAM_CODEC.decode(buffer);
            assertEquals(id, decoded.tabletId());
            assertEquals(action, decoded.action());
        }
    }

    @Test
    void theClockCapIsAQuarterOfTheComputerClock() {
        assertEquals(li.cil.oc2.common.Constants.CPU_FREQUENCY / 4, TabletVirtualMachine.MAX_FREQUENCY);
    }
}
