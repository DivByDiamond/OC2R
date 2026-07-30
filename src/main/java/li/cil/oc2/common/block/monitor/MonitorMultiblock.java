package li.cil.oc2.common.block.monitor;

import javax.annotation.Nullable;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Helper for the expandable multiblock monitor system (similar to OpenComputers 1.12.2).
 * <p>
 * Each monitor block stores four BlockState properties describing its position inside the
 * multiblock:
 * <ul>
 *     <li>{@link MonitorBlock#WIDTH}  - total width of the multiblock (1..{@link #MAX_WIDTH}).</li>
 *     <li>{@link MonitorBlock#HEIGHT} - total height of the multiblock (1..{@link #MAX_HEIGHT}).</li>
 *     <li>{@link MonitorBlock#ORIGIN_OFFSET_X} - this block's column offset from the origin (0..WIDTH-1).</li>
 *     <li>{@link MonitorBlock#ORIGIN_OFFSET_Y} - this block's row offset from the origin (0..HEIGHT-1).</li>
 * </ul>
 * <p>
 * Convention: the origin (offset 0,0) is at the <b>top-right</b> corner of the multiblock from
 * the viewer's point of view (i.e. the player standing in front of the screen). The width axis
 * grows toward the viewer's left, the height axis grows downward. This matches the existing
 * {@link li.cil.oc2.client.renderer.blockentity.MonitorRenderer} which already draws the
 * framebuffer from the top-right corner extending left and down, so enabling multiblock only
 * requires scaling that drawing by {@code width} and {@code height}.
 * <p>
 * Only the origin block hosts a "live" {@link li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity};
 * all other blocks are inert sub-blocks whose BlockEntity exists only so Minecraft can keep
 * their state. They never run server ticks, never expose capabilities and never render the
 * framebuffer themselves.
 */
public final class MonitorMultiblock {
    /** Maximum width of a monitor multiblock, in blocks. */
    public static final int MAX_WIDTH = 8;

    /** Maximum height of a monitor multiblock, in blocks. */
    public static final int MAX_HEIGHT = 8;

    private MonitorMultiblock() {}

    /**
     * The horizontal direction (in world space) along which the multiblock's width extends.
     * Offset (0,0) is the top-right corner; offset_x grows toward the viewer's left.
     */
    public static Direction getWidthDir(final Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;  // viewer faces south, viewer's left = +X = EAST
            case SOUTH -> Direction.WEST;  // viewer faces north, viewer's left = -X = WEST
            case EAST -> Direction.SOUTH;  // viewer faces west,  viewer's left = +Z = SOUTH
            case WEST -> Direction.NORTH;  // viewer faces east,  viewer's left = -Z = NORTH
            default -> throw new IllegalArgumentException("Monitor must be horizontal, got " + facing);
        };
    }

    /**
     * The vertical direction (in world space) along which the multiblock's height extends.
     * Always {@link Direction#DOWN} because the origin is at the top row and offset_y grows
     * downward.
     */
    public static Direction getHeightDir() {
        return Direction.DOWN;
    }

    /** Returns {@code true} when the given state belongs to the origin (top-right) of its multiblock. */
    public static boolean isOrigin(final BlockState state) {
        return state.getValue(MonitorBlock.ORIGIN_OFFSET_X) == 0
                && state.getValue(MonitorBlock.ORIGIN_OFFSET_Y) == 0;
    }

    /** Returns {@code true} when the given state is part of a multiblock larger than 1x1. */
    public static boolean isPartOfMultiblock(final BlockState state) {
        return state.getValue(MonitorBlock.WIDTH) > 1
                || state.getValue(MonitorBlock.HEIGHT) > 1;
    }

    /**
     * Compute the world position of the origin block (offset 0,0) of the multiblock containing
     * the block at {@code pos} whose state is {@code state}.
     */
    public static BlockPos getOriginPos(final BlockPos pos, final BlockState state) {
        final int offsetX = state.getValue(MonitorBlock.ORIGIN_OFFSET_X);
        final int offsetY = state.getValue(MonitorBlock.ORIGIN_OFFSET_Y);
        final Direction widthDir = getWidthDir(state.getValue(MonitorBlock.FACING));
        final Direction heightDir = getHeightDir();
        // origin = pos - offsetX * widthDir - offsetY * heightDir
        return pos.relative(widthDir.getOpposite(), offsetX)
                .relative(heightDir.getOpposite(), offsetY);
    }

    /**
     * Compute the world position of the block at the given (offsetX, offsetY) inside the
     * multiblock whose origin is at {@code origin} and which faces {@code facing}.
     */
    public static BlockPos getBlockPos(
            final BlockPos origin, final Direction facing, final int offsetX, final int offsetY) {
        final Direction widthDir = getWidthDir(facing);
        final Direction heightDir = getHeightDir();
        return origin.relative(widthDir, offsetX).relative(heightDir, offsetY);
    }

    /**
     * Try to merge a freshly placed monitor at {@code pos} (already carrying the default 1x1
     * multiblock BlockState) into an adjacent existing multiblock facing the same direction.
     * <p>
     * The four supported merge directions are: extend left (no origin shift), extend down
     * (no origin shift), extend right at top row (origin shifts to the new block), extend up
     * at leftmost column (origin shifts to the new block). Corner extensions are intentionally
     * not supported — the player must break and rebuild to grow the multiblock in those
     * directions.
     *
     * @return {@code true} if the new block was merged into an existing multiblock.
     */
    public static boolean tryMergeIntoMultiblock(final Level level, final BlockPos pos, final Direction facing) {
        final Direction widthDir = getWidthDir(facing);
        final Direction heightDir = getHeightDir();
        // Four neighbors in the multiblock plane: +widthDir (left of new block, viewer POV),
        // -widthDir (right of new block), +heightDir (below), -heightDir (above).
        // Note: heightDir is DOWN, so -heightDir is UP.
        final Direction[] neighbors = {
            widthDir, widthDir.getOpposite(), heightDir, heightDir.getOpposite()
        };

        for (final Direction dir : neighbors) {
            final BlockPos neighborPos = pos.relative(dir);
            final BlockState neighborState = level.getBlockState(neighborPos);
            if (!(neighborState.getBlock() instanceof MonitorBlock)) continue;
            if (neighborState.getValue(MonitorBlock.FACING) != facing) continue;

            final BlockPos originPos = getOriginPos(neighborPos, neighborState);
            final BlockState originState = level.getBlockState(originPos);
            if (!(originState.getBlock() instanceof MonitorBlock)) continue;

            final int W = originState.getValue(MonitorBlock.WIDTH);
            final int H = originState.getValue(MonitorBlock.HEIGHT);

            // Compute P's would-be (offset_x, offset_y) relative to the existing origin.
            final int pOx = dot(pos.subtract(originPos), widthDir);
            final int pOy = dot(pos.subtract(originPos), heightDir);

            // Extend LEFT (new leftmost column from viewer POV): origin stays, W' = W + 1.
            if (pOx == W && pOy >= 0 && pOy < H && W < MAX_WIDTH) {
                applyMultiblockSize(level, originPos, originState, W + 1, H);
                setSubBlockState(level, pos, facing, W + 1, H, W, pOy);
                return true;
            }
            // Extend DOWN (new bottom row): origin stays, H' = H + 1.
            if (pOy == H && pOx >= 0 && pOx < W && H < MAX_HEIGHT) {
                applyMultiblockSize(level, originPos, originState, W, H + 1);
                setSubBlockState(level, pos, facing, W, H + 1, pOx, H);
                return true;
            }
            // Extend RIGHT at top row (origin shifts to new block): W' = W + 1, all offsets +1 in X.
            if (pOx == -1 && pOy == 0 && W < MAX_WIDTH) {
                shiftOriginAndGrow(level, originPos, originState, pos, facing, W + 1, H, true);
                return true;
            }
            // Extend UP at leftmost column (origin shifts to new block): H' = H + 1, all offsets +1 in Y.
            if (pOy == -1 && pOx == 0 && H < MAX_HEIGHT) {
                shiftOriginAndGrow(level, originPos, originState, pos, facing, W, H + 1, false);
                return true;
            }
        }
        return false;
    }

    /**
     * Break the whole multiblock containing the block at {@code pos}. Drops {@code W*H} monitor
     * items at {@code pos} and silently removes every other block of the multiblock. The block
     * at {@code pos} itself is left in place — Minecraft will remove it as part of the normal
     * destroy flow.
     * <p>
     * This must be idempotent: callers protect re-entrancy with
     * {@link MonitorBlock#IS_BREAKING_MULTIBLOCK}.
     */
    public static void breakMultiblock(final Level level, final BlockPos pos, final BlockState state) {
        final BlockPos originPos = isOrigin(state) ? pos : getOriginPos(pos, state);
        final BlockState originState = level.getBlockState(originPos);
        if (!(originState.getBlock() instanceof MonitorBlock)) {
            return;
        }

        final int W = originState.getValue(MonitorBlock.WIDTH);
        final int H = originState.getValue(MonitorBlock.HEIGHT);
        final Direction facing = originState.getValue(MonitorBlock.FACING);

        // Drop W*H items at the broken block's position (where the player can pick them up).
        if (!level.isClientSide()) {
            Block.popResource(level, pos, new ItemStack(Items.MONITOR.get(), W * H));
        }

        // Remove every other block of the multiblock silently.
        for (int ox = 0; ox < W; ox++) {
            for (int oy = 0; oy < H; oy++) {
                final BlockPos blockPos = getBlockPos(originPos, facing, ox, oy);
                if (blockPos.equals(pos)) continue;
                // Drop the BE so its resources don't get re-dropped by destroyBlock.
                final BlockEntity be = level.getBlockEntity(blockPos);
                if (be != null) {
                    be.setRemoved();
                }
                level.removeBlock(blockPos, false);
            }
        }
    }

    /**
     * Look up the origin {@link MonitorBlockEntity} for the multiblock containing the block at
     * {@code pos}. Returns {@code null} if the block at {@code pos} isn't a monitor, the
     * origin isn't loaded, or doesn't have a BlockEntity.
     */
    @Nullable
    public static li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity getOriginEntity(
            final Level level, final BlockPos pos, final BlockState state) {
        if (isOrigin(state)) {
            final BlockEntity be = level.getBlockEntity(pos);
            return be instanceof li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity m
                    ? m : null;
        }
        final BlockPos originPos = getOriginPos(pos, state);
        if (!level.isLoaded(originPos)) return null;
        final BlockEntity be = level.getBlockEntity(originPos);
        return be instanceof li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity m
                ? m : null;
    }

    // ---- internals ---------------------------------------------------------

    private static int dot(final BlockPos v, final Direction d) {
        return v.getX() * d.getStepX() + v.getY() * d.getStepY() + v.getZ() * d.getStepZ();
    }

    private static void applyMultiblockSize(
            final Level level, final BlockPos originPos, final BlockState originState,
            final int newW, final int newH) {
        final BlockState newState = originState
                .setValue(MonitorBlock.WIDTH, newW)
                .setValue(MonitorBlock.HEIGHT, newH);
        level.setBlock(originPos, newState, Block.UPDATE_CLIENTS);
    }

    private static void setSubBlockState(
            final Level level, final BlockPos pos, final Direction facing,
            final int width, final int height, final int offsetX, final int offsetY) {
        final BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof MonitorBlock)) return;
        final BlockState newState = current
                .setValue(MonitorBlock.FACING, facing)
                .setValue(MonitorBlock.WIDTH, width)
                .setValue(MonitorBlock.HEIGHT, height)
                .setValue(MonitorBlock.ORIGIN_OFFSET_X, offsetX)
                .setValue(MonitorBlock.ORIGIN_OFFSET_Y, offsetY);
        level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
    }

    /**
     * Shift the origin to {@code newOriginPos} (which is currently the freshly placed block)
     * and grow the multiblock by one in the requested axis. The persistent state of the old
     * origin's BlockEntity (energy, power, deviceId, capture input) is transferred to the new
     * origin's BlockEntity so the multiblock keeps functioning without interruption.
     *
     * @param growWidth {@code true} to grow width (offset_x of every existing block += 1),
     *                  {@code false} to grow height (offset_y of every existing block += 1).
     */
    private static void shiftOriginAndGrow(
            final Level level, final BlockPos oldOriginPos, final BlockState oldOriginState,
            final BlockPos newOriginPos, final Direction facing,
            final int newW, final int newH, final boolean growWidth) {
        final int oldW = oldOriginState.getValue(MonitorBlock.WIDTH);
        final int oldH = oldOriginState.getValue(MonitorBlock.HEIGHT);

        // Capture the old origin's persistent state so we can re-apply it to the new origin.
        final net.minecraft.world.level.block.entity.BlockEntity oldBE = level.getBlockEntity(oldOriginPos);
        final net.minecraft.nbt.CompoundTag transferredState;
        final net.minecraft.core.HolderLookup.Provider registries = resolveRegistries(level);
        if (oldBE instanceof li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity oldMonitor && registries != null) {
            transferredState = oldMonitor.saveStateForTransfer(registries);
        } else {
            transferredState = null;
        }

        // Re-stamp every existing block of the multiblock with the new dimensions and bumped offset.
        for (int ox = 0; ox < oldW; ox++) {
            for (int oy = 0; oy < oldH; oy++) {
                final BlockPos blockPos = getBlockPos(oldOriginPos, facing, ox, oy);
                final BlockState s = level.getBlockState(blockPos);
                if (!(s.getBlock() instanceof MonitorBlock)) continue;
                final int newOx = growWidth ? ox + 1 : ox;
                final int newOy = growWidth ? oy : oy + 1;
                final BlockState newState = s
                        .setValue(MonitorBlock.WIDTH, newW)
                        .setValue(MonitorBlock.HEIGHT, newH)
                        .setValue(MonitorBlock.ORIGIN_OFFSET_X, newOx)
                        .setValue(MonitorBlock.ORIGIN_OFFSET_Y, newOy);
                level.setBlock(blockPos, newState, Block.UPDATE_CLIENTS);
            }
        }

        // The freshly placed block (new origin) is currently carrying the default 1x1 state.
        // Stamp it with origin (0,0) + new dimensions.
        final BlockState newOriginCurrent = level.getBlockState(newOriginPos);
        if (newOriginCurrent.getBlock() instanceof MonitorBlock) {
            final BlockState newState = newOriginCurrent
                    .setValue(MonitorBlock.FACING, facing)
                    .setValue(MonitorBlock.WIDTH, newW)
                    .setValue(MonitorBlock.HEIGHT, newH)
                    .setValue(MonitorBlock.ORIGIN_OFFSET_X, 0)
                    .setValue(MonitorBlock.ORIGIN_OFFSET_Y, 0);
            level.setBlock(newOriginPos, newState, Block.UPDATE_CLIENTS);
        }

        // Transfer the persistent state from the old origin to the new origin. The freshly
        // placed block already has a BlockEntity (created during placement) — we just need to
        // load the transferred state into it.
        if (transferredState != null && registries != null) {
            final net.minecraft.world.level.block.entity.BlockEntity newBE = level.getBlockEntity(newOriginPos);
            if (newBE instanceof li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity newMonitor) {
                newMonitor.loadStateFromTransfer(transferredState, registries);
            }
        }
    }

    /**
     * Best-effort lookup of a {@link net.minecraft.core.HolderLookup.Provider} from a
     * {@link Level}. On the server side this is the server's registry access; on the client
     * side (where this code should never run, since multiblock merges only happen in
     * {@link MonitorBlock#setPlacedBy} which is gated on {@code !level.isClientSide()}) we
     * fall back to {@code null} and skip the state transfer.
     */
    @javax.annotation.Nullable
    private static net.minecraft.core.HolderLookup.Provider resolveRegistries(final Level level) {
        // In Minecraft 1.21+ Level implements HolderLookup.Provider indirectly via LevelAccessor,
        // so the instanceof check covers both singleplayer and dedicated server cases.
        if (level instanceof net.minecraft.core.HolderLookup.Provider provider) {
            return provider;
        }
        final net.minecraft.server.MinecraftServer server = level.getServer();
        return server != null ? server.registryAccess() : null;
    }
}
