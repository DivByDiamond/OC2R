package li.cil.oc2.common.blockentity.projector;

import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ProjectorStateMessage;
import li.cil.oc2.jcodec.common.model.Picture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

final class ProjectorState {
    boolean isMounted;
    boolean hasEnergy;

    boolean isProjecting(final Level level, final BlockPos pos, final BlockState state) {
        if (!isMounted || level == null) {
            return false;
        }

        final Direction facing = state.getValue(ProjectorBlock.FACING);
        final BlockPos neighborPos = pos.relative(facing);
        final int neighborChunkX = SectionPos.blockToSectionCoord(neighborPos.getX());
        final int neighborChunkZ = SectionPos.blockToSectionCoord(neighborPos.getZ());
        return level.hasChunk(neighborChunkX, neighborChunkZ);
    }

    void applyClient(final boolean projecting, final boolean energy) {
        isMounted = projecting;
        hasEnergy = energy;
    }

    void update(final Level level, final BlockPos pos, final BlockState state,
                final Picture picture, final boolean newIsMounted, final boolean newHasEnergy,
                final boolean isValid, final ProjectorBlockEntity be) {
        if ((newIsMounted == isMounted && newHasEnergy == hasEnergy) || !isValid) {
            return;
        }

        if (level != null && !level.isClientSide() && level.isLoaded(pos)) {
            if (isMounted && !newIsMounted) {
                Arrays.fill(picture.getPlaneData(0), (byte) -128);
                Arrays.fill(picture.getPlaneData(1), (byte) 0);
                Arrays.fill(picture.getPlaneData(2), (byte) 0);
            }

            isMounted = newIsMounted;
            hasEnergy = newHasEnergy;

            level.setBlock(pos, state.setValue(ProjectorBlock.LIT, newIsMounted), Block.UPDATE_CLIENTS);

            Network.sendToClientsTrackingBlockEntity(new ProjectorStateMessage(be, newIsMounted, newHasEnergy), be);
        }
    }
}
