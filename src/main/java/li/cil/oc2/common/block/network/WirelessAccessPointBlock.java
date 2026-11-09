package li.cil.oc2.common.block.network;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.BlockCodecs;
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

/** A floor-mounted radio access point with an Ethernet socket on its underside. */
public final class WirelessAccessPointBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape NORTH_SHAPE = Block.box(2, 0, 2, 14, 2, 14);
    private static final VoxelShape EAST_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NORTH_SHAPE);
    private static final VoxelShape SOUTH_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(EAST_SHAPE);
    private static final VoxelShape WEST_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(SOUTH_SHAPE);

    public WirelessAccessPointBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f).noOcclusion());
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<WirelessAccessPointBlock> codec() {
        return BlockCodecs.WIRELESS_ACCESS_POINT.get();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return BlockEntities.WIRELESS_ACCESS_POINT.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
            final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.WIRELESS_ACCESS_POINT.get());
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}
