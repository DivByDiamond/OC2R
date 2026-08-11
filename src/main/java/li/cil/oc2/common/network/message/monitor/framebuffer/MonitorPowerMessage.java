package li.cil.oc2.common.network.message.monitor.framebuffer;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MonitorPowerMessage(BlockPos pos, boolean power) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, MonitorPowerMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    MonitorPowerMessage::pos,
                    ByteBufCodecs.BOOL,
                    MonitorPowerMessage::power,
                    MonitorPowerMessage::new);

    public static final CustomPacketPayload.Type<MonitorPowerMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "monitor_power_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public MonitorPowerMessage(final MonitorBlockEntity monitor, final boolean power) {
        this(monitor.getBlockPos(), power);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(
                context,
                pos,
                MonitorBlockEntity.class,
                (player, monitor) -> {
                    if (power) {
                        monitor.start();
                    } else {
                        monitor.stop();
                    }
                    NetworkMessages.sendToClientsTrackingBlockEntity(
                            new MonitorPowerMessageForwarded(monitor, power), monitor);
                });
    }
}