package li.cil.oc2.common.network.message.projector;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import li.cil.oc2.common.network.message.misc.AbstractMessage;

public record ProjectorStateMessage(BlockPos pos, boolean isMounted, boolean hasEnergy)
        implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ProjectorStateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ProjectorStateMessage::pos,
                    ByteBufCodecs.BOOL,
                    ProjectorStateMessage::isMounted,
                    ByteBufCodecs.BOOL,
                    ProjectorStateMessage::hasEnergy,
                    ProjectorStateMessage::new);

    public static final CustomPacketPayload.Type<ProjectorStateMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "projector_state_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ProjectorStateMessage(
            final ProjectorBlockEntity projector,
            final boolean isMounted,
            final boolean hasEnergy) {
        this(projector.getBlockPos(), isMounted, hasEnergy);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos,
                ProjectorBlockEntity.class,
                projector -> projector.applyProjectorStateClient(isMounted, hasEnergy));
    }
}