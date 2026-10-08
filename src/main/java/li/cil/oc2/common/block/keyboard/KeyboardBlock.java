package li.cil.oc2.common.block.keyboard;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.block.common.BlockCodecs;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.hooks.ClientProxy;
import li.cil.oc2.common.util.block.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class KeyboardBlock extends HorizontalDirectionalBlock implements EntityBlock {
    // The model is a wall panel rotated by the blockstate (x: -90) so it lies flat on the floor;
    // this is the same slab, 14x8 wide and 1 high, in block space.
    private static final VoxelShape NEG_Z_SHAPE = Block.box(1, 0, 4, 15, 1, 12);
    private static final VoxelShape NEG_X_SHAPE =
            VoxelShapeUtils.rotateHorizontalClockwise(NEG_Z_SHAPE);
    private static final VoxelShape POS_Z_SHAPE =
            VoxelShapeUtils.rotateHorizontalClockwise(NEG_X_SHAPE);
    private static final VoxelShape POS_X_SHAPE =
            VoxelShapeUtils.rotateHorizontalClockwise(POS_Z_SHAPE);

    public KeyboardBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<KeyboardBlock> codec() {
        return BlockCodecs.KEYBOARD.get();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NEG_Z_SHAPE;
            case SOUTH -> POS_Z_SHAPE;
            case WEST -> NEG_X_SHAPE;
            default -> POS_X_SHAPE;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final BlockHitResult hitResult) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final KeyboardBlockEntity keyboard)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (level.isClientSide()) {
            openKeyboardScreen(keyboard);
        }

        //? if >=26.1 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResult.sidedSuccess(level.isClientSide());
        //?}
    }

    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.KEYBOARD.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(
            final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    private static void openKeyboardScreen(final KeyboardBlockEntity keyboard) {
        ClientProxy.get().openKeyboardScreen(keyboard);
    }
}