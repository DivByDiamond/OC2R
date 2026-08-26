package li.cil.oc2.common.blockentity.network.cable.facade;

import javax.annotation.Nullable;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class FacadeManager {
    private final BusCableBlockEntity owner;
    private ItemStack facade = ItemStack.EMPTY;

    public FacadeManager(final BusCableBlockEntity owner) {
        this.owner = owner;
    }

    public ItemStack getFacade() {
        return facade;
    }

    public FacadeType getFacadeType(final ItemStack stack) {
        return getFacadeType(ItemStackUtils.getBlockState(stack));
    }

    public FacadeType getFacadeType(@Nullable final BlockState state) {
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

    public void setFacade(final ItemStack stack) {
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
        // Setting HAS_FACADE triggers a vanilla sendBlockUpdated internally, whose
        // implicit BlockEntityDataPacket already carries getUpdateTag (facade included)
        // to all tracking clients. No extra channels are needed.
        BusCableStateProperties.setHasFacade(
                level, owner.getBlockPos(), owner.getBlockState(), facadeState, true);

        owner.setChanged();
        owner.requestModelDataUpdate();
    }

    public void removeFacade() {
        final var level = owner.getLevel();
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(facade);
        facade = ItemStack.EMPTY;
        BusCableStateProperties.setHasFacade(
                level, owner.getBlockPos(), owner.getBlockState(), facadeState, false);

        owner.setChanged();
        owner.requestModelDataUpdate();
    }

    public void setFacadeDirectly(final ItemStack stack) {
        facade = stack;
    }
}