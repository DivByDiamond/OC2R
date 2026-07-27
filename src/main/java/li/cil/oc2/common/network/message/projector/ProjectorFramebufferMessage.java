package li.cil.oc2.common.network.message.projector;

import java.nio.ByteBuffer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import li.cil.oc2.common.util.nbt.Oc2rStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import li.cil.oc2.common.network.message.misc.AbstractMessage;

public record ProjectorFramebufferMessage(BlockPos pos, ByteBuffer frame)
        implements AbstractMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectorFramebufferMessage>
            STREAM_CODEC =
                    StreamCodec.composite(
                            BlockPos.STREAM_CODEC,
                            ProjectorFramebufferMessage::pos,
                            Oc2rStreamCodecs.BYTE_BUFFER,
                            ProjectorFramebufferMessage::frame,
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
                projector -> projector.applyNextFrameClient(frame));
    }
}