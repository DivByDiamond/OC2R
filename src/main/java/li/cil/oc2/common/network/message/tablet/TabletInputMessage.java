package li.cil.oc2.common.network.message.tablet;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Keyboard and mouse bytes for the guest, accepted only from a player viewing that tablet. */
public record TabletInputMessage(UUID tabletId, byte[] data) implements AbstractMessage {
    /** Largest single packet; a screen sends a few bytes per key press. */
    public static final int MAX_BYTES = 4096;

    public TabletInputMessage {
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    public static final StreamCodec<ByteBuf, TabletInputMessage> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    TabletInputMessage::tabletId,
                    ByteBufCodecs.byteArray(MAX_BYTES),
                    TabletInputMessage::data,
                    TabletInputMessage::new);

    public static final CustomPacketPayload.Type<TabletInputMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "tablet_input_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public TabletInputMessage(final UUID tabletId, final ByteBuffer input) {
        this(tabletId, copyRemaining(input));
    }

    private static byte[] copyRemaining(final ByteBuffer input) {
        final byte[] copy = new byte[input.remaining()];
        input.duplicate().get(copy);
        return copy;
    }

    @Override
    public void handleMessage(final MessageContext context) {
        if (!(context.player() instanceof final ServerPlayer player) || data.length > MAX_BYTES) return;
        TabletManager.viewedMachine(player, tabletId).ifPresent(machine -> machine.putInput(data));
    }
}
