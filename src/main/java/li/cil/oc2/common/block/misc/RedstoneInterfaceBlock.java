package li.cil.oc2.common.block.misc;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.misc.RedstoneInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import li.cil.oc2.common.block.common.BlockCodecs;

public final class RedstoneInterfaceBlock extends HorizontalDirectionalBlock
        implements EntityBlock {
    public RedstoneInterfaceBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<RedstoneInterfaceBlock> codec() {
        return BlockCodecs.REDSTONE_INTERFACE.get();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final Direction side) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final RedstoneInterfaceBlockEntity redstoneInterface) {
            // Redstone requests info for faces with external perspective. We treat
            // the Direction from internal perspective, so flip it.
            return redstoneInterface.getOutputForDirection(side.getOpposite());
        }

        return super.getSignal(state, level, pos, side);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override
    @SuppressWarnings({"deprecation", "DataFlowIssue"})
    public void neighborChanged(
            BlockState state,
            Level worldIn,
            BlockPos pos,
            Block blockIn,
            BlockPos fromPos,
            boolean isMoving) {
        RedstoneInterfaceBlockEntity ribe =
                (RedstoneInterfaceBlockEntity) worldIn.getBlockEntity(pos);
        ribe.neighborChanged(fromPos);
    }

    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.REDSTONE_INTERFACE.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(
            final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}