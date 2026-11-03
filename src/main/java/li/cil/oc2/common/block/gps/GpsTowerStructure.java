package li.cil.oc2.common.block.gps;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Five blocks in one loaded chunk, mutated as a single structure. */
public final class GpsTowerStructure {
    public static final int HEIGHT = 5;

    private record Mutation(Level level, BlockPos base) {}
    private static final Set<Mutation> MUTATIONS = ConcurrentHashMap.newKeySet();

    /** Returns whether the whole structure, including its foundation, is present. */
    public static boolean isComplete(final Level level, final BlockPos base) {
        if (!level.hasChunkAt(base)) return false;
        final BlockState first = level.getBlockState(base);
        if (!(first.getBlock() instanceof GpsTowerBlock) || first.getValue(GpsTowerBlock.PART) != 0) return false;
        for (int i = 1; i < HEIGHT; i++) {
            if (!matches(level.getBlockState(base.above(i)), first, i)) return false;
        }
        return level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP);
    }

    static boolean canPlace(final BlockPlaceContext context, final BlockState baseState) {
        final Level level = context.getLevel();
        final BlockPos base = context.getClickedPos();
        for (int i = 0; i < HEIGHT; i++) {
            final BlockPos pos = base.above(i);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                    || !level.getWorldBorder().isWithinBounds(pos)) return false;
            final BlockPlaceContext partContext = BlockPlaceContext.at(context, pos, Direction.UP);
            if (!level.getBlockState(pos).canBeReplaced(partContext)) return false;
            final var player = context.getPlayer();
            if (player != null && (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, Direction.UP, context.getItemInHand()))) return false;
            if (!level.isUnobstructed(baseState.setValue(GpsTowerBlock.PART, i), pos,
                    player == null ? CollisionContext.empty() : CollisionContext.of(player))) return false;
        }
        return level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP);
    }

    static boolean place(final BlockPlaceContext context, final BlockState state) {
        if (!canPlace(context, state)) return false;
        final Level level = context.getLevel();
        final BlockPos base = context.getClickedPos();
        final BlockState[] previous = new BlockState[HEIGHT];
        for (int i = 0; i < HEIGHT; i++) previous[i] = level.getBlockState(base.above(i));
        final Mutation mutation = new Mutation(level, base.immutable());
        if (!MUTATIONS.add(mutation)) return false;
        boolean success = false;
        try {
            for (int i = 0; i < HEIGHT; i++) {
                if (!level.setBlock(base.above(i), state.setValue(GpsTowerBlock.PART, i),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) return false;
            }
            success = true;
        } finally {
            try {
                if (!success) {
                    for (int i = 0; i < HEIGHT; i++) {
                        level.setBlock(base.above(i), previous[i], Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            } finally {
                MUTATIONS.remove(mutation);
            }
        }
        for (int i = 0; i < HEIGHT; i++) level.updateNeighborsAt(base.above(i), state.getBlock());
        return true;
    }

    static void removed(final Level level, final BlockPos pos, final BlockState removed) {
        if (level.isClientSide()) return;
        final int part = removed.getValue(GpsTowerBlock.PART);
        final BlockPos base = pos.below(part);
        if (level instanceof final net.minecraft.server.level.ServerLevel server) {
            li.cil.oc2.common.blockentity.gps.GpsNetworkData.get(server).removeAt(base);
            li.cil.oc2.common.blockentity.radio.RadioNetworkData.get(server).removeAt(base);
        }
        final Mutation mutation = new Mutation(level, base.immutable());
        if (!MUTATIONS.add(mutation)) return;
        try {
            for (int i = 0; i < HEIGHT; i++) {
                final BlockPos target = base.above(i);
                if (i != part && matches(level.getBlockState(target), removed, i)) {
                    // The originally destroyed part owns the one vanilla loot roll.
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        } finally {
            MUTATIONS.remove(mutation);
        }
    }

    private static boolean matches(final BlockState state, final BlockState reference, final int part) {
        return state.is(reference.getBlock()) && state.getValue(GpsTowerBlock.PART) == part
                && state.getValue(GpsTowerBlock.FACING) == reference.getValue(GpsTowerBlock.FACING);
    }

    private GpsTowerStructure() {}
}
