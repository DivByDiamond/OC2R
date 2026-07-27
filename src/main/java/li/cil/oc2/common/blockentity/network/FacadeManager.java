package li.cil.oc2.common.blockentity.network;

import javax.annotation.Nullable;
import li.cil.oc2.common.block.BusCableStateProperties;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusCableFacadeMessage;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

final class FacadeManager {
    private final BusCableBlockEntity owner;
    private ItemStack facade = ItemStack.EMPTY;

    FacadeManager(final BusCableBlockEntity owner) {
        this.owner = owner;
    }

    ItemStack getFacade() {
        return facade;
    }

    FacadeType getFacadeType(final ItemStack stack) {
        return getFacadeType(ItemStackUtils.getBlockState(stack));
    }

    FacadeType getFacadeType(@Nullable final BlockState state) {
        if (state == null) {
            return FacadeType.NOT_A_BLOCK;
        }

        final var level = owner.getLevel();
        if (level == null
                || state.getRenderShape() != RenderShape.MODEL
                || !state.isSolidRender(level, owner.getBlockPos())
                || state.getBlock() instanceof EntityBlock) {
            return FacadeType.INVALID_BLOCK;
        }

        return FacadeType.VALID_BLOCK;
    }

    void setFacade(final ItemStack stack) {
        final var level = owner.getLevel();
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(stack);
        final ItemStack effectiveStack;
        if (getFacadeType(facadeState) != FacadeType.VALID_BLOCK) {
            effectiveStack = ItemStack.EMPTY;
        } else {
            effectiveStack = stack;
        }

        if (ItemStack.isSameItem(effectiveStack, facade)) {
            return;
        }

        facade = effectiveStack.copy();
        facade.setCount(1);
        BusCableStateProperties.setHasFacade(
                level, owner.getBlockPos(), owner.getBlockState(), facadeState, true);

        owner.setChanged();
        level.sendBlockUpdated(
                owner.getBlockPos(),
                owner.getBlockState(),
                owner.getBlockState(),
                Block.UPDATE_ALL);

        if (!level.isClientSide()) {
            final BusCableFacadeMessage message =
                    new BusCableFacadeMessage(owner.getBlockPos(), facade);
            Network.sendToClientsTrackingBlockEntity(message, owner);
        }

        owner.requestModelDataUpdate();
    }

    void removeFacade() {
        final var level = owner.getLevel();
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(facade);
        facade = ItemStack.EMPTY;
        BusCableStateProperties.setHasFacade(
                level, owner.getBlockPos(), owner.getBlockState(), facadeState, false);

        owner.setChanged();
        level.sendBlockUpdated(
                owner.getBlockPos(),
                owner.getBlockState(),
                owner.getBlockState(),
                Block.UPDATE_ALL);

        if (!level.isClientSide()) {
            final BusCableFacadeMessage message =
                    new BusCableFacadeMessage(owner.getBlockPos(), facade);
            Network.sendToClientsTrackingBlockEntity(message, owner);
        }

        owner.requestModelDataUpdate();
    }

    void setFacadeDirectly(final ItemStack stack) {
        facade = stack;
    }
}