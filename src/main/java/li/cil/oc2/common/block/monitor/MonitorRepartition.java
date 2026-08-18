package li.cil.oc2.common.block.monitor;

import java.util.Collection;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Re-stamps a collection of monitor blocks into a single full WxH rectangle.
 */
public final class MonitorRepartition {
    private MonitorRepartition() {}

    /**
     * Re-stamp the given monitor blocks (all facing {@code facing}) as one full rectangle.
     * <p>
     * The rectangle's origin becomes the block with the smallest column and row offset along the
     * multiblock axes (the top-right corner). Each block keeps its world position and gets fresh
     * WIDTH/HEIGHT/ORIGIN_OFFSET_X/ORIGIN_OFFSET_Y properties. The persistent monitor state
     * (deviceId, energy, capture input) is carried over to the new origin: from
     * {@code stateSource} if given, otherwise from whichever block in {@code blocks} is currently
     * the live origin of a multiblock.
     *
     * @param stateSource block whose BlockEntity holds the state to transfer, or {@code null} to
     *                    auto-detect; used when the previous origin is being removed.
     */
    public static void repartition(
            final Level level, final Collection<BlockPos> blocks, final Direction facing,
            @Nullable final BlockPos stateSource) {
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
        final BlockPos newOrigin = base.relative(widthDir, minCx - baseCx)
                .relative(heightDir, minCy - baseCy);

        final HolderLookup.Provider registries = MonitorMultiblock.resolveRegistries(level);
        final CompoundTag savedState = saveState(level, stateSource, blocks, newOrigin, registries);

        for (final BlockPos bp : blocks) {
            final BlockState current = level.getBlockState(bp);
            if (!(current.getBlock() instanceof MonitorBlock)) continue;
            final int cx = MonitorMultiblock.dot(bp, widthDir);
            final int cy = MonitorMultiblock.dot(bp, heightDir);
            final BlockState newState = current
                    .setValue(MonitorBlock.FACING, facing)
                    .setValue(MonitorBlock.WIDTH, width)
                    .setValue(MonitorBlock.HEIGHT, height)
                    .setValue(MonitorBlock.ORIGIN_OFFSET_X, cx - minCx)
                    .setValue(MonitorBlock.ORIGIN_OFFSET_Y, cy - minCy);
            level.setBlock(bp, newState, Block.UPDATE_CLIENTS);
        }

        if (savedState != null && registries != null) {
            final BlockEntity be = level.getBlockEntity(newOrigin);
            if (be instanceof MonitorBlockEntity monitor) {
                monitor.loadStateFromTransfer(savedState, registries);
            }
        }
    }

    @Nullable
    private static CompoundTag saveState(
            final Level level, @Nullable final BlockPos stateSource,
            final Collection<BlockPos> blocks, final BlockPos newOrigin,
            final HolderLookup.Provider registries) {
        BlockEntity source = stateSource != null ? level.getBlockEntity(stateSource) : null;
        if (!(source instanceof MonitorBlockEntity)) {
            source = findLiveOrigin(level, blocks, newOrigin);
        }
        if (source instanceof MonitorBlockEntity monitor && registries != null) {
            return monitor.saveStateForTransfer(registries);
        }
        return null;
    }

    @Nullable
    private static BlockEntity findLiveOrigin(
            final Level level, final Collection<BlockPos> blocks, final BlockPos newOrigin) {
        if (isLiveOrigin(level, newOrigin)) {
            return level.getBlockEntity(newOrigin);
        }
        for (final BlockPos bp : blocks) {
            if (isLiveOrigin(level, bp)) {
                return level.getBlockEntity(bp);
            }
        }
        return null;
    }

    private static boolean isLiveOrigin(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof MonitorBlock
                && MonitorMultiblock.isOrigin(state)
                && level.getBlockEntity(pos) instanceof MonitorBlockEntity;
    }
}
