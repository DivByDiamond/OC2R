package li.cil.oc2.common.network.message.projector;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import li.cil.oc2.common.network.util.frame.FrameChunker;
import li.cil.oc2.common.util.nbt.Oc2rStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ProjectorFramebufferMessage(
        BlockPos pos,
        int width,
        int height,
        int chunkIndex,
        int chunkCount,
        byte[] data) implements AbstractMessage {

    public static final StreamCodec<FriendlyByteBuf, ProjectorFramebufferMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ProjectorFramebufferMessage::pos,
                    ByteBufCodecs.VAR_INT,
                    ProjectorFramebufferMessage::width,
                    ByteBufCodecs.VAR_INT,
                    ProjectorFramebufferMessage::height,
                    ByteBufCodecs.VAR_INT,
                    ProjectorFramebufferMessage::chunkIndex,
                    ByteBufCodecs.VAR_INT,
                    ProjectorFramebufferMessage::chunkCount,
                    Oc2rStreamCodecs.BYTE_ARRAY,
                    ProjectorFramebufferMessage::data,
                    ProjectorFramebufferMessage::new);

    public static final CustomPacketPayload.Type<ProjectorFramebufferMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "projector_framebuffer_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos,
                ProjectorBlockEntity.class,
                projector -> projector.applyChunk(
                        width, height, chunkIndex, chunkCount, data));
    }
}
