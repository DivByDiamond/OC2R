/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.util.Oc2rStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.ByteBuffer;

public record MonitorFramebufferMessage(BlockPos pos, ByteBuffer frame) implements AbstractMessage {
    public static final StreamCodec<FriendlyByteBuf, MonitorFramebufferMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        MonitorFramebufferMessage::pos,
        Oc2rStreamCodecs.BYTE_BUFFER,
        MonitorFramebufferMessage::frame,
        MonitorFramebufferMessage::new
    );

    public static final CustomPacketPayload.Type<MonitorFramebufferMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "monitor_framebuffer_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientBlockEntityAt(pos, MonitorBlockEntity.class,
            monitor -> monitor.applyNextFrameClient(frame));
    }
}
