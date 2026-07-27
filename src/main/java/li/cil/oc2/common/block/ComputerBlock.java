package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;

import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.TooltipUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

import javax.annotation.Nullable;

public final class ComputerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public ComputerBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<ComputerBlock> codec() {
        return BlockCodecs.COMPUTER.get();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final Item.TooltipContext context,
            final List<Component> tooltip,
            final TooltipFlag advanced) {
        super.appendHoverText(stack, context, tooltip, advanced);
        TooltipUtils.addEnergyConsumption(Config.computerEnergyPerTick, tooltip);
        TooltipUtils.addInventoryInformation(stack, tooltip);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    public int getSignal(
            final BlockState state,
            final BlockGetter blockGetter,
            final BlockPos pos,
            final Direction side) {
        final int signal = ComputerBlockRedstone.getSignal(blockGetter, pos, side);
        return signal >= 0 ? signal : super.getSignal(state, blockGetter, pos, side);
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

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Block changedBlock,
            final BlockPos changedBlockPos,
            final boolean isMoving) {
        ComputerBlockRedstone.neighborChanged(level, pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(
            final BlockState state,
            final BlockGetter level,
            final BlockPos pos,
            final CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> ComputerBlockShapes.NEG_Z_SHAPE;
            case SOUTH -> ComputerBlockShapes.POS_Z_SHAPE;
            case WEST -> ComputerBlockShapes.NEG_X_SHAPE;
            default -> ComputerBlockShapes.POS_X_SHAPE;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(
            final ItemStack stack,
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final ComputerBlockEntity computer)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (Wrenches.isWrench(stack)) {
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide() && player instanceof final ServerPlayer serverPlayer) {
                    computer.openInventoryScreen(serverPlayer);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final BlockHitResult hitResult) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final ComputerBlockEntity computer)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                computer.start();
            } else if (player instanceof final ServerPlayer serverPlayer) {
                computer.openTerminalScreen(serverPlayer);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BlockState playerWillDestroy(
            final Level level, final BlockPos pos, final BlockState state, final Player player) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!level.isClientSide() && blockEntity instanceof final ComputerBlockEntity computer) {
            if (!computer.getItemStackHandlers().isEmpty()) {
                if (player.isCreative()) {
                    final ItemStack stack = new ItemStack(Items.COMPUTER.get());
                    computer.exportToItemStack(stack);
                    popResource(level, pos, stack);
                }
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.COMPUTER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createTicker(level, type, BlockEntities.COMPUTER.get());
    }

    @Override
    protected void createBlockStateDefinition(
            final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
