package li.cil.oc2.common.energy;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class EnergyTransferManager {
    private EnergyTransferManager() {}

    public static void distribute(final BusCableBlockEntity cable) {
        final Level level = cable.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        final BlockPos pos = cable.getBlockPos();
        cable.energy.pullFromNeighbors(level, pos);
        cable.energy.transferToNeighbors(level, pos);
    }

    public static Set<BlockPos> collectNetwork(final Level level, final BlockPos origin) {
        final Set<BlockPos> visited = new LinkedHashSet<>();
        final Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        while (!queue.isEmpty()) {
            final BlockPos pos = queue.poll();
            if (!visited.add(pos)) {
                continue;
            }
            for (final Direction side : Direction.values()) {
                final BlockPos neighborPos = pos.relative(side);
                if (isCable(level, neighborPos, side.getOpposite())) {
                    queue.add(neighborPos);
                }
            }
        }
        return visited;
    }

    private static boolean isCable(
            final Level level, final BlockPos pos, final Direction facing) {
        final BlockState state = level.getBlockState(pos);
        if (!state.getBlock().equals(Blocks.BUS_CABLE.get())
                || !state.getValue(BusCableStateProperties.HAS_CABLE)) {
            return false;
        }
        final ConnectionType connectionType =
                BusCableStateProperties.getConnectionType(state, facing);
        return connectionType == ConnectionType.CABLE || connectionType == ConnectionType.INTERFACE;
    }
}