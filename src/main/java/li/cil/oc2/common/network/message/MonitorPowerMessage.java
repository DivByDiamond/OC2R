/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.MonitorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MonitorPowerMessage(BlockPos pos, boolean power) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, MonitorPowerMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        MonitorPowerMessage::pos,
        ByteBufCodecs.BOOL,
        MonitorPowerMessage::power,
        MonitorPowerMessage::new
    );

    public static final CustomPacketPayload.Type<MonitorPowerMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "monitor_power_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public MonitorPowerMessage(final MonitorBlockEntity monitor, final boolean power) {
        this(monitor.getBlockPos(), power);
    }

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, MonitorBlockEntity.class,
            (player, monitor) -> {
                if (power) {
                    monitor.start();
                } else {
                    monitor.stop();
                }
                Network.sendToClientsTrackingBlockEntity(new MonitorPowerMessageForwarded(monitor, power), monitor);
            });
    }
}
