package li.cil.oc2.common.block.misc;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.provider.SidedBlockDevice;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.util.block.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.*;

/** A wall-mounted local environment probe exposed through the device bus. */
public final class EnvironmentSensorBlock extends HorizontalDirectionalBlock implements EntityBlock, SidedBlockDevice {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape NORTH_SHAPE = Block.box(5, 6, 12, 13, 11, 16);
    // This legacy helper rotates the north model toward west in world coordinates.
    private static final VoxelShape WEST_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NORTH_SHAPE);
    private static final VoxelShape SOUTH_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(WEST_SHAPE);
    private static final VoxelShape EAST_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(SOUTH_SHAPE);

    public EnvironmentSensorBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f).noOcclusion());
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<EnvironmentSensorBlock> codec() {
        return BlockCodecs.ENVIRONMENT_SENSOR.get();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isVertical()) return null;
        final BlockState state = defaultBlockState().setValue(FACING, clicked);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(final BlockState state, final net.minecraft.world.level.LevelReader level,
            final BlockPos pos) {
        final Direction facing = state.getValue(FACING);
        final BlockPos support = pos.relative(facing.getOpposite());
        final BlockState backing = level.getBlockState(support);
        return backing.isFaceSturdy(level, support, facing)
                || backing.is(Blocks.BUS_CABLE.get())
                        && (backing.getValue(BusCableStateProperties.HAS_CABLE)
                                || BusCableStateProperties.getConnectionType(backing, facing)
                                        == li.cil.oc2.common.block.types.ConnectionType.INTERFACE);
    }

    @Override
    //? if >=26.1 {
    /*public BlockState updateShape(final BlockState state, final net.minecraft.world.level.LevelReader level,
            final net.minecraft.world.level.ScheduledTickAccess ticks, final BlockPos pos,
            final Direction side, final BlockPos neighborPos, final BlockState neighborState,
            final net.minecraft.util.RandomSource random) {
    *///?} else {
    public BlockState updateShape(final BlockState state, final Direction side, final BlockState neighborState,
            final net.minecraft.world.level.LevelAccessor level, final BlockPos pos, final BlockPos neighborPos) {
    //?}
        return side == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public boolean canConnectDevice(final BlockState state, @Nullable final Direction side) {
        return side == state.getValue(FACING).getOpposite();
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
            final CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.ENVIRONMENT_SENSOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.ENVIRONMENT_SENSOR.get());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}
