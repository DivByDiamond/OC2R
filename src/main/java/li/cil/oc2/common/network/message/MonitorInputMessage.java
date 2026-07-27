package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MonitorInputMessage(BlockPos pos, int keycode, boolean isDown)
        implements AbstractMessage {
    public static final StreamCodec<ByteBuf, MonitorInputMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    MonitorInputMessage::pos,
                    ByteBufCodecs.INT,
                    MonitorInputMessage::keycode,
                    ByteBufCodecs.BOOL,
                    MonitorInputMessage::isDown,
                    MonitorInputMessage::new);

    public static final CustomPacketPayload.Type<MonitorInputMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "monitor_input_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public MonitorInputMessage(
            final MonitorBlockEntity keyboard, final int keycode, final boolean isDown) {
        this(keyboard.getBlockPos(), keycode, isDown);
    }

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(
                context,
                pos,
                MonitorBlockEntity.class,
                (player, monitor) -> monitor.handleInput(keycode, isDown));
    }
}