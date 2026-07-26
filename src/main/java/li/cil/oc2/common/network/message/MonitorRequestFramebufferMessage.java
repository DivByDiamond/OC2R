/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.MonitorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.MonitorLoadBalancer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MonitorRequestFramebufferMessage(BlockPos pos) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, MonitorRequestFramebufferMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        MonitorRequestFramebufferMessage::pos,
        MonitorRequestFramebufferMessage::new
    );

    public static final CustomPacketPayload.Type<MonitorRequestFramebufferMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "monitor_request_framebuffer_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public MonitorRequestFramebufferMessage(final MonitorBlockEntity projector) {
        this(projector.getBlockPos());
    }

    ///////////////////////////////////////////////////////////////////

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntity(context, pos, MonitorBlockEntity.class,
            (player, monitor) -> MonitorLoadBalancer.updateWatcher(monitor, player));
    }
}
