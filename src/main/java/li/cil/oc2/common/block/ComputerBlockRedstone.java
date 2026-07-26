package li.cil.oc2.common.block;

import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

final class ComputerBlockRedstone {
    static int getSignal(final BlockGetter blockGetter, final BlockPos pos, final Direction side) {
        final BlockEntity blockEntity = blockGetter.getBlockEntity(pos);
        if (blockEntity != null) {
            var level = blockEntity.getLevel();
            if (level != null) {
                var cap = level.getCapability(Capabilities.RedstoneEmitter.BLOCK, blockEntity.getBlockPos(), null, blockEntity, side.getOpposite());
                return Optional.ofNullable(cap)
                    .map(RedstoneEmitter::getRedstoneOutput)
                    .orElse(0);
            }
        }
        return -1;
    }

    static void neighborChanged(final Level level, final BlockPos pos) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final ComputerBlockEntity computer) {
            computer.handleNeighborChanged();
        }
    }
}
