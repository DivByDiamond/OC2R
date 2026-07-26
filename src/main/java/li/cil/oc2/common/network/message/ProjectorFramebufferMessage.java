/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.util.Oc2rStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.ByteBuffer;

public record ProjectorFramebufferMessage(BlockPos pos, ByteBuffer frame) implements AbstractMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectorFramebufferMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ProjectorFramebufferMessage::pos,
        Oc2rStreamCodecs.BYTE_BUFFER,
        ProjectorFramebufferMessage::frame,
        ProjectorFramebufferMessage::new
    );

    public static final CustomPacketPayload.Type<ProjectorFramebufferMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "projector_framebuffer_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ProjectorBlockEntity.class,
            projector -> projector.applyNextFrameClient(frame));
    }
}
