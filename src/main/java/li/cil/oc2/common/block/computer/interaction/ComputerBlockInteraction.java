package li.cil.oc2.common.block.computer.interaction;

import javax.annotation.Nullable;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=26.1 {
/*import net.minecraft.world.InteractionResult;
*///?} else {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ComputerBlockInteraction {

    //? if >=26.1 {
    /*public static InteractionResult useItemOn(
    *///?} else {
    public static ItemInteractionResult useItemOn(
    //?}
            final ItemStack stack,
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final InteractionHand hand,
            final BlockHitResult hitResult,
            @Nullable final ComputerBlockEntity computer) {
        if (computer == null) {
            //? if >=26.1 {
            /*return InteractionResult.TRY_WITH_EMPTY_HAND;
            *///?} else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            //?}
        }

        if (!Wrenches.isWrench(stack)) {
            //? if >=26.1 {
            /*return InteractionResult.TRY_WITH_EMPTY_HAND;
            *///?} else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            //?}
        }
        if (player.isShiftKeyDown()) {
            //? if >=26.1 {
            /*return InteractionResult.PASS;
            *///?} else {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            //?}
        }
        if (!level.isClientSide() && player instanceof final ServerPlayer serverPlayer) {
            computer.terminalManager.openInventoryScreen(serverPlayer);
        }
        //? if >=26.1 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
        //?}
    }

    public static InteractionResult useWithoutItem(
            final BlockState state,
            final Level level,
            final BlockPos pos,
            final Player player,
            final BlockHitResult hitResult,
            @Nullable final ComputerBlockEntity computer) {
        if (computer == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                computer.terminalManager.start();
            } else if (player instanceof final ServerPlayer serverPlayer) {
                if (hitResult.getDirection() == state.getValue(HorizontalDirectionalBlock.FACING)) {
                    computer.terminalManager.openTerminalScreen(serverPlayer);
                } else {
                    computer.terminalManager.openInventoryScreen(serverPlayer);
                }
            }
        }

        //? if >=26.1 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResult.sidedSuccess(level.isClientSide());
        //?}
    }

    public static BlockState playerWillDestroy(
            final Level level, final BlockPos pos, final BlockState state, final Player player,
            @Nullable final ComputerBlockEntity computer) {
        if (!level.isClientSide()
                && computer != null
                && !computer.getItemStackHandlers().isEmpty()
                && player.isCreative()) {
            final ItemStack stack = new ItemStack(Items.COMPUTER.get());
            computer.exportToItemStack(stack);
            Block.popResource(level, pos, stack);
        }
        return state;
    }
}
