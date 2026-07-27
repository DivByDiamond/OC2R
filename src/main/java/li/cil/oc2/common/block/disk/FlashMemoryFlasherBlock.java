package li.cil.oc2.common.block.disk;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.misc.FlashMemoryFlasherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import li.cil.oc2.common.block.common.BlockCodecs;

public final class FlashMemoryFlasherBlock extends HorizontalDirectionalBlock
        implements EntityBlock {
    public FlashMemoryFlasherBlock() {
        super(Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<FlashMemoryFlasherBlock> codec() {
        return BlockCodecs.FLASH_MEMORY_FLASHER.get();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        if (!(blockEntity instanceof FlashMemoryFlasherBlockEntity diskDrive)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (!player.isShiftKeyDown()) {
            if (diskDrive.canInsert(stack)) {
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, diskDrive.insert(stack, player));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
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
        if (!(blockEntity instanceof final FlashMemoryFlasherBlockEntity diskDrive)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (player.isShiftKeyDown()) {
            if (diskDrive.canEject()) {
                if (!level.isClientSide()) {
                    diskDrive.eject(player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public BlockState playerWillDestroy(
            final Level level, final BlockPos pos, final BlockState state, final Player player) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!level.isClientSide()
                && blockEntity instanceof final FlashMemoryFlasherBlockEntity flashFlasher) {
            if (!flashFlasher.getDiskItemStack().isEmpty()) {
                final ItemStack stack = flashFlasher.getDiskItemStack();
                popResource(level, pos, stack);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.FLASH_MEMORY_FLASHER.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(
            final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}