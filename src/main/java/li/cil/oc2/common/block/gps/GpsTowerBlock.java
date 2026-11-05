package li.cil.oc2.common.block.gps;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.gps.GpsTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A single registered block with five persisted part states. */
public final class GpsTowerBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 4);

    public GpsTowerBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.5f, 6.0f)
                .noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected MapCodec<GpsTowerBlock> codec() {
        return BlockCodecs.GPS_TOWER.get();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return GpsTowerStructure.canPlace(context, state) ? state : null;
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        return GpsTowerShapes.outline(state.getValue(PART));
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                        final CollisionContext context) {
        return GpsTowerShapes.collision(state.getValue(PART));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return state.getValue(PART) == 0 ? BlockEntities.GPS_TOWER.get().create(pos, state) : null;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rotation) {
        // A single-part wrench rotation must not split the structure's facing states.
        return state;
    }

    //? if >=26.1 {
    /*@Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level,
                                               final BlockPos pos, final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (!level.getBlockState(pos).is(this)) GpsTowerStructure.removed(level, pos, state);
    }
    *///?} else {
    @Override
    public void onRemove(final BlockState state, final Level level, final BlockPos pos,
                         final BlockState next, final boolean movedByPiston) {
        super.onRemove(state, level, pos, next, movedByPiston);
        if (!next.is(this)) GpsTowerStructure.removed(level, pos, state);
    }
    //?}

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos,
                        final BlockState previous, final boolean movedByPiston) {
        super.onPlace(state, level, pos, previous, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    //? if >=26.1 {
    /*@Override
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION",
            justification = "Vanilla passes a null orientation; the parameter is not used.")
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                final Block changedBlock,
                                @Nullable final net.minecraft.world.level.redstone.Orientation orientation,
                                final boolean movedByPiston) {
    *///?} else {
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                final Block changedBlock, final BlockPos changedPos, final boolean movedByPiston) {
    //?}
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (!GpsTowerStructure.isComplete(level, pos.below(state.getValue(PART)))) {
            level.destroyBlock(pos, true);
        } else if (state.getValue(PART) == 0
                && level.getBlockEntity(pos) instanceof final GpsTowerBlockEntity tower) {
            tower.refreshNetwork();
        }
    }
}
