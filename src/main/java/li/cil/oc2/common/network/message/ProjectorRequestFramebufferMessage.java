package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.ProjectorLoadBalancer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ProjectorRequestFramebufferMessage(BlockPos pos) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ProjectorRequestFramebufferMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ProjectorRequestFramebufferMessage::pos,
                    ProjectorRequestFramebufferMessage::new);

    public static final CustomPacketPayload.Type<ProjectorRequestFramebufferMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "projector_request_framebuffer_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ProjectorRequestFramebufferMessage(final ProjectorBlockEntity projector) {
        this(projector.getBlockPos());
    }

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntity(
                context,
                pos,
                ProjectorBlockEntity.class,
                (player, projector) -> ProjectorLoadBalancer.updateWatcher(projector, player));
    }
}