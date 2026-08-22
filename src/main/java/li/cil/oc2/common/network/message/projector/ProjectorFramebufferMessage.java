package li.cil.oc2.common.network.message.projector;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import li.cil.oc2.common.util.nbt.Oc2rStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ProjectorFramebufferMessage(
        BlockPos pos,
        int codec,
        int width,
        int height,
        int frameSize,
        int chunkIndex,
        int chunkCount,
        byte[] data) implements AbstractMessage {

    public static final StreamCodec<FriendlyByteBuf, ProjectorFramebufferMessage> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ProjectorFramebufferMessage decode(final FriendlyByteBuf buf) {
                    return new ProjectorFramebufferMessage(
                            BlockPos.STREAM_CODEC.decode(buf),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            Oc2rStreamCodecs.BYTE_ARRAY.decode(buf));
                }

                @Override
                public void encode(
                        final FriendlyByteBuf buf, final ProjectorFramebufferMessage message) {
                    BlockPos.STREAM_CODEC.encode(buf, message.pos());
                    buf.writeVarInt(message.codec());
                    buf.writeVarInt(message.width());
                    buf.writeVarInt(message.height());
                    buf.writeVarInt(message.frameSize());
                    buf.writeVarInt(message.chunkIndex());
                    buf.writeVarInt(message.chunkCount());
                    Oc2rStreamCodecs.BYTE_ARRAY.encode(buf, message.data());
                }
            };

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
                        codec, width, height, frameSize, chunkIndex, chunkCount, data));
    }
}
