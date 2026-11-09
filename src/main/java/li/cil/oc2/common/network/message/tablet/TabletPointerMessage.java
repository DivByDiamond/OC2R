package li.cil.oc2.common.network.message.tablet;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Absolute pointer, key or release operation for one currently viewed tablet. */
public record TabletPointerMessage(UUID tabletId, int operation, int first, int second, int value)
        implements AbstractMessage {
    public static final int POINTER = 0;
    public static final int KEY = 1;
    public static final int RELEASE = 2;
    public static final StreamCodec<ByteBuf, TabletPointerMessage> STREAM_CODEC = new StreamCodec<>() {
    
    @Override public TabletPointerMessage decode(final ByteBuf buffer) {
            return new TabletPointerMessage(UUIDUtil.STREAM_CODEC.decode(buffer), buffer.readUnsignedByte(),
                    buffer.readInt(), buffer.readInt(), buffer.readInt());
        }
    
    @Override public void encode(final ByteBuf buffer, final TabletPointerMessage message) {
            UUIDUtil.STREAM_CODEC.encode(buffer, message.tabletId());
            buffer.writeByte(message.operation()).writeInt(message.first())
                    .writeInt(message.second()).writeInt(message.value());
        }
    };
    public static final CustomPacketPayload.Type<TabletPointerMessage> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "tablet_pointer"));

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    @Override public void handleMessage(final MessageContext context) {
        if (!(context.player() instanceof final ServerPlayer player)) return;
        TabletManager.viewedMachine(player, tabletId).ifPresent(machine -> {
            switch (operation) {
                case POINTER -> machine.display().pointer(first, second, value);
                case KEY -> { if (value == 0 || value == 1) machine.display().key(first, value == 1); }
                case RELEASE -> machine.display().releaseInput();
                default -> { }
            }
        });
    }
}
