package li.cil.oc2.common.block.monitor;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import li.cil.oc2.common.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Merges a freshly placed monitor block into adjacent same-facing monitor multiblocks.
 */
public final class MonitorMerge {
    private MonitorMerge() {}

    /**
     * Try to merge the freshly placed monitor at {@code pos} (currently a 1x1 block) into the
     * surrounding monitors facing the same direction.
     * <p>
     * All monitors reachable from {@code pos} through any 4-adjacent same-facing monitor (each
     * expanded over its full multiblock footprint) are collected. The merge only happens if the
     * bounding box of the collected blocks is completely filled — a full WxH rectangle — and the
     * result does not exceed the configured maximum dimensions. Blocks may be placed in any
     * order; if the union is not a full rectangle the new block simply stays a 1x1 monitor.
     *
     * @return {@code true} if the new block was merged into a multiblock.
     */
    public static boolean tryMergeIntoMultiblock(
            final Level level, final BlockPos pos, final Direction facing) {
        final Set<BlockPos> blocks = collect(level, pos, facing);
        final Direction widthDir = MonitorMultiblock.getWidthDir(facing);
        final Direction heightDir = MonitorMultiblock.getHeightDir();

        final BlockPos base = blocks.iterator().next();
        final int baseCx = MonitorMultiblock.dot(base, widthDir);
        final int baseCy = MonitorMultiblock.dot(base, heightDir);

        int minCx = baseCx;
        int minCy = baseCy;
        int maxCx = baseCx;
        int maxCy = baseCy;
        for (final BlockPos bp : blocks) {
            final int cx = MonitorMultiblock.dot(bp, widthDir);
            final int cy = MonitorMultiblock.dot(bp, heightDir);
            minCx = Math.min(minCx, cx);
            minCy = Math.min(minCy, cy);
            maxCx = Math.max(maxCx, cx);
            maxCy = Math.max(maxCy, cy);
        }
        final int width = maxCx - minCx + 1;
        final int height = maxCy - minCy + 1;
        if (width < 2 && height < 2) {
            return false;
        }
        if (width > Config.monitorMaxWidth || height > Config.monitorMaxHeight) {
            return false;
        }

        final BlockPos newOrigin = base.relative(widthDir, minCx - baseCx)
                .relative(heightDir, minCy - baseCy);
        for (int ox = 0; ox < width; ox++) {
            for (int oy = 0; oy < height; oy++) {
                if (!blocks.contains(MonitorMultiblock.getBlockPos(newOrigin, facing, ox, oy))) {
                    return false;
                }
            }
        }

        MonitorRepartition.repartition(level, blocks, facing, null);
        return true;
    }

    private static Set<BlockPos> collect(
            final Level level, final BlockPos start, final Direction facing) {
        final Set<BlockPos> collected = new HashSet<>();
        final Set<BlockPos> visited = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            expandBlock(level, queue.poll(), facing, collected, visited, queue);
        }
        return collected;
    }

    private static void expandBlock(
            final Level level,
            final BlockPos pos,
            final Direction facing,
            final Set<BlockPos> collected,
            final Set<BlockPos> visited,
            final ArrayDeque<BlockPos> queue) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MonitorBlock)) return;
        if (state.getValue(MonitorBlock.FACING) != facing) return;
        collected.add(pos);

        queueMultiblockFootprint(level, pos, state, facing, visited, queue);

        for (final Direction dir : Direction.values()) {
            final BlockPos neighbor = pos.relative(dir);
            if (visited.add(neighbor)) {
                queue.add(neighbor);
            }
        }
    }

    private static void queueMultiblockFootprint(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            final Direction facing,
            final Set<BlockPos> visited,
            final ArrayDeque<BlockPos> queue) {
        final BlockPos origin = MonitorMultiblock.getOriginPos(pos, state);
        final BlockState originState = level.getBlockState(origin);
        if (originState.getBlock() instanceof MonitorBlock
                && originState.getValue(MonitorBlock.FACING) == facing) {
            final int w = originState.getValue(MonitorBlock.WIDTH);
            final int h = originState.getValue(MonitorBlock.HEIGHT);
            for (int ox = 0; ox < w; ox++) {
                for (int oy = 0; oy < h; oy++) {
                    final BlockPos cell = MonitorMultiblock.getBlockPos(origin, facing, ox, oy);
                    if (visited.add(cell)) {
                        queue.add(cell);
                    }
                }
            }
        }
    }
}
