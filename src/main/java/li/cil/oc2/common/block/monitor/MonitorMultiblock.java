package li.cil.oc2.common.block.monitor;

import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Coordinate helpers for the multiblock monitor system.
 * <p>
 * Each monitor block stores four BlockState properties describing its position inside the
 * multiblock: {@link MonitorBlock#WIDTH}, {@link MonitorBlock#HEIGHT},
 * {@link MonitorBlock#ORIGIN_OFFSET_X} and {@link MonitorBlock#ORIGIN_OFFSET_Y}. The origin
 * (offset 0,0) is at the <b>top-right</b> corner of the multiblock from the viewer's point of
 * view; the width axis grows toward the viewer's left, the height axis grows downward. The
 * multiblock is always a full WxH rectangle, and only the origin hosts a live
 * {@link MonitorBlockEntity} (deviceId, energy, capture input, framebuffer); all other blocks
 * are inert sub-blocks.
 * <p>
 * Structure edits — merging a freshly placed block ( {@link MonitorMerge}), breaking a single
 * block ({@link MonitorBreak}) and re-stamping layouts ({@link MonitorRepartition}) — are
 * implemented in the dedicated classes.
 */
public final class MonitorMultiblock {
    /**
     * Upper bound of the {@code mb_width}/{@code mb_height} blockstate properties. Kept fixed
     * rather than read from the config so existing blockstates stay valid; the actual formation
     * limit is {@link li.cil.oc2.common.config.Config#monitorMaxWidth} /
     * {@link li.cil.oc2.common.config.Config#monitorMaxHeight}.
     */
    public static final int MAX_WIDTH = 8;

    /** Upper bound of the {@code mb_width}/{@code mb_height} blockstate properties. */
    public static final int MAX_HEIGHT = 8;

    private MonitorMultiblock() {}

    /**
     * The horizontal direction (in world space) along which the multiblock's width extends.
     * Offset (0,0) is the top-right corner; offset_x grows toward the viewer's left.
     */
    public static Direction getWidthDir(final Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
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
     * Look up the origin {@link MonitorBlockEntity} for the multiblock containing the block at
     * {@code pos}. Returns {@code null} if the block at {@code pos} isn't a monitor, the
     * origin isn't loaded, or doesn't have a BlockEntity.
     */
    @Nullable
    public static MonitorBlockEntity getOriginEntity(
            final Level level, final BlockPos pos, final BlockState state) {
        if (isOrigin(state)) {
            final BlockEntity be = level.getBlockEntity(pos);
            return be instanceof MonitorBlockEntity monitor ? monitor : null;
        }
        final BlockPos originPos = getOriginPos(pos, state);
        if (!level.isLoaded(originPos)) return null;
        final BlockEntity be = level.getBlockEntity(originPos);
        return be instanceof MonitorBlockEntity monitor ? monitor : null;
    }

    // ---- internals ---------------------------------------------------------

    static int dot(final BlockPos v, final Direction d) {
        return v.getX() * d.getStepX() + v.getY() * d.getStepY() + v.getZ() * d.getStepZ();
    }

    /**
     * Best-effort lookup of a {@link HolderLookup.Provider} from a {@link Level}. This only runs
     * server-side (merge/break are gated on {@code !level.isClientSide()}), but we fall back to
     * {@code null} and skip the state transfer if no registries can be resolved.
     */
    @Nullable
    static HolderLookup.Provider resolveRegistries(final Level level) {
        if (level instanceof HolderLookup.Provider provider) {
            return provider;
        }
        final MinecraftServer server = level.getServer();
        return server != null ? server.registryAccess() : null;
    }
}
