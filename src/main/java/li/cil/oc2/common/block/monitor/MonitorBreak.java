package li.cil.oc2.common.block.monitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Handles breaking a single block of a monitor multiblock.
 */
public final class MonitorBreak {
    private MonitorBreak() {}

    /**
     * Re-partition the blocks remaining after the monitor at {@code pos} (whose state is
     * {@code state}) is removed.
     * <p>
     * Only the clicked block is destroyed; the survivors are split into connected components and
     * each component is re-stamped as one or more full rectangles (holes are cut out by
     * repeatedly extracting the largest full rectangle). The persistent monitor state follows
     * the previous origin, or the block nearest to it when the origin itself was broken.
     */
    public static void onBlockBroken(
            final Level level, final BlockPos pos, final BlockState state) {
        final Direction facing = state.getValue(MonitorBlock.FACING);
        final BlockPos originPos = MonitorMultiblock.isOrigin(state)
                ? pos : MonitorMultiblock.getOriginPos(pos, state);
        final BlockState originState = level.getBlockState(originPos);
        if (!(originState.getBlock() instanceof MonitorBlock)) return;
        final int width = originState.getValue(MonitorBlock.WIDTH);
        final int height = originState.getValue(MonitorBlock.HEIGHT);
        if (width * height == 1) return;

        final Set<BlockPos> remaining = collectRemaining(originPos, facing, width, height, pos);

        final boolean brokenWasOrigin = MonitorMultiblock.isOrigin(state);
        final BlockPos stateSource = brokenWasOrigin ? pos : originPos;
        final BlockPos anchor = brokenWasOrigin ? findAnchor(remaining, pos, facing) : originPos;

        for (final Set<BlockPos> component : connectedComponents(remaining)) {
            repartitionComponent(level, component, facing, stateSource, anchor);
        }
    }

    private static Set<BlockPos> collectRemaining(
            final BlockPos originPos,
            final Direction facing,
            final int width,
            final int height,
            final BlockPos pos) {
        final Set<BlockPos> remaining = new HashSet<>();
        for (int ox = 0; ox < width; ox++) {
            for (int oy = 0; oy < height; oy++) {
                final BlockPos bp = MonitorMultiblock.getBlockPos(originPos, facing, ox, oy);
                if (!bp.equals(pos)) {
                    remaining.add(bp);
                }
            }
        }
        return remaining;
    }

    @Nullable
    private static BlockPos findAnchor(
            final Set<BlockPos> remaining, final BlockPos originPos, final Direction facing) {
        final Direction widthDir = MonitorMultiblock.getWidthDir(facing);
        final Direction heightDir = MonitorMultiblock.getHeightDir();
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (final BlockPos bp : remaining) {
            final int ox = MonitorMultiblock.dot(bp.subtract(originPos), widthDir);
            final int oy = MonitorMultiblock.dot(bp.subtract(originPos), heightDir);
            final int distance = ox + oy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = bp;
            }
        }
        return best;
    }

    private static List<Set<BlockPos>> connectedComponents(final Set<BlockPos> blocks) {
        final List<Set<BlockPos>> components = new ArrayList<>();
        final Set<BlockPos> visited = new HashSet<>();
        for (final BlockPos start : blocks) {
            if (!visited.add(start)) continue;
            components.add(collectComponent(blocks, start, visited));
        }
        return components;
    }

    private static Set<BlockPos> collectComponent(
            final Set<BlockPos> blocks, final BlockPos start, final Set<BlockPos> visited) {
        final Set<BlockPos> component = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            final BlockPos cur = queue.poll();
            component.add(cur);
            for (final Direction dir : Direction.values()) {
                final BlockPos neighbor = cur.relative(dir);
                if (blocks.contains(neighbor) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return component;
    }

    private static void repartitionComponent(
            final Level level, final Set<BlockPos> cells, final Direction facing,
            @Nullable final BlockPos stateSource, @Nullable final BlockPos anchor) {
        final Direction widthDir = MonitorMultiblock.getWidthDir(facing);
        final Direction heightDir = MonitorMultiblock.getHeightDir();
        final BlockPos base = cells.iterator().next();
        final int baseCx = MonitorMultiblock.dot(base, widthDir);
        final int baseCy = MonitorMultiblock.dot(base, heightDir);

        int minCx = baseCx;
        int minCy = baseCy;
        int maxCx = baseCx;
        int maxCy = baseCy;
        for (final BlockPos bp : cells) {
            final int cx = MonitorMultiblock.dot(bp, widthDir);
            final int cy = MonitorMultiblock.dot(bp, heightDir);
            minCx = Math.min(minCx, cx);
            minCy = Math.min(minCy, cy);
            maxCx = Math.max(maxCx, cx);
            maxCy = Math.max(maxCy, cy);
        }
        final int width = maxCx - minCx + 1;
        final int height = maxCy - minCy + 1;
        final BlockPos origin = base.relative(widthDir, minCx - baseCx)
                .relative(heightDir, minCy - baseCy);

        final BlockPos source = anchor != null && cells.contains(anchor) ? stateSource : null;
        if (cells.size() == width * height) {
            MonitorRepartition.repartition(level, cells, facing, source);
            return;
        }

        final Set<BlockPos> rectangle = findBestRectangle(cells, origin, facing, width, height);
        final BlockPos rectSource = anchor != null && rectangle.contains(anchor) ? stateSource : null;
        MonitorRepartition.repartition(level, rectangle, facing, rectSource);

        final Set<BlockPos> rest = new HashSet<>(cells);
        rest.removeAll(rectangle);
        for (final Set<BlockPos> component : connectedComponents(rest)) {
            repartitionComponent(level, component, facing, stateSource, anchor);
        }
    }

    private static Set<BlockPos> findBestRectangle(
            final Set<BlockPos> cells,
            final BlockPos origin,
            final Direction facing,
            final int width,
            final int height) {
        int bestX0 = 0;
        int bestY0 = 0;
        int bestX1 = 0;
        int bestY1 = 0;
        int bestArea = 0;
        for (int x0 = 0; x0 < width; x0++) {
            for (int y0 = 0; y0 < height; y0++) {
                final BestRectangle candidate =
                        findBestRectangleAt(cells, origin, facing, x0, y0, width, height, bestArea);
                if (candidate != null) {
                    bestX0 = x0;
                    bestY0 = y0;
                    bestX1 = candidate.x1();
                    bestY1 = candidate.y1();
                    bestArea = candidate.area();
                }
            }
        }
        return buildRectangle(origin, facing, bestX0, bestY0, bestX1, bestY1);
    }

    @Nullable
    private static BestRectangle findBestRectangleAt(
            final Set<BlockPos> cells,
            final BlockPos origin,
            final Direction facing,
            final int x0,
            final int y0,
            final int width,
            final int height,
            final int minArea) {
        int bestX1 = -1;
        int bestY1 = -1;
        int bestArea = minArea;
        for (int x1 = x0; x1 < width; x1++) {
            for (int y1 = y0; y1 < height; y1++) {
                final int area = (x1 - x0 + 1) * (y1 - y0 + 1);
                if (area > bestArea && isFull(cells, origin, facing, x0, y0, x1, y1)) {
                    bestX1 = x1;
                    bestY1 = y1;
                    bestArea = area;
                }
            }
        }
        if (bestX1 == -1) {
            return null;
        }
        return new BestRectangle(bestX1, bestY1, bestArea);
    }

    private static Set<BlockPos> buildRectangle(
            final BlockPos origin,
            final Direction facing,
            final int x0,
            final int y0,
            final int x1,
            final int y1) {
        final Set<BlockPos> rectangle = new HashSet<>();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                rectangle.add(MonitorMultiblock.getBlockPos(origin, facing, x, y));
            }
        }
        return rectangle;
    }

    private record BestRectangle(int x1, int y1, int area) {}

    private static boolean isFull(
            final Set<BlockPos> cells, final BlockPos origin, final Direction facing,
            final int x0, final int y0, final int x1, final int y1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                if (!cells.contains(MonitorMultiblock.getBlockPos(origin, facing, x, y))) {
                    return false;
                }
            }
        }
        return true;
    }
}
