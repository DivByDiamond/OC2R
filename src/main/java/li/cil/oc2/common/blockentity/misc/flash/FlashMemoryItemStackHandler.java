package li.cil.oc2.common.blockentity.misc.flash;

import static li.cil.oc2.common.item.block.AbstractBlockDeviceItem.DATA_TAG_NAME;

import javax.annotation.Nonnull;
import li.cil.oc2.common.container.handler.TypedItemStackHandler;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.item.ItemStackUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

class FlashMemoryItemStackHandler extends TypedItemStackHandler {
    private final FlashMemoryFlasherBlockEntity owner;

    public FlashMemoryItemStackHandler(final FlashMemoryFlasherBlockEntity owner) {
        super(1, ItemTags.DEVICES_FLASH_MEMORY);
        this.owner = owner;
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        // Deserialization does not call onContentsChanged. Restore the backing handle now,
        // before a later update-tag/export can overwrite the item's data with an empty handle.
        // This only imports metadata: levels are not necessarily attached yet, and clients
        // must not open or close server world blob files while receiving an update tag.
        final ItemStack stack = getStackInSlotRaw(0);
        if (!stack.isEmpty()) {
            //? if >=26.1 {
            /*owner.device.importFromItemStack(ItemStackUtils.getModDataTag(stack).getCompoundOrEmpty(DATA_TAG_NAME));
            *///?} else {
            owner.device.importFromItemStack(ItemStackUtils.getModDataTag(stack).getCompound(DATA_TAG_NAME));
            //?}
        }
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return !owner.isWriting() && !stack.isEmpty()
                && (stack.is(ItemTags.DEVICES_FLASH_MEMORY)
                    || stack.getItem() instanceof li.cil.oc2.common.item.storage.HardDriveItem);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (owner.isWriting()) return ItemStack.EMPTY;
        if (slot == 0 && !simulate && amount > 0) {
            exportDeviceDataToItemStack(getStackInSlotRaw(0));
        }
        return super.extractItem(slot, amount, simulate);
    }

    public ItemStack getStackInSlotRaw(final int slot) {
        return super.getStackInSlot(slot);
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(final int slot) {
        final ItemStack stack = getStackInSlotRaw(slot);
        exportDeviceDataToItemStack(stack);
        return stack;
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 1;
    }

    @Override
    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        exportDeviceDataToItemStack(getStackInSlotRaw(0));
        return super.serializeNBT(provider);
    }

    @Override
    protected void onContentsChanged(final int slot) {
        super.onContentsChanged(slot);

        var level = owner.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        final ItemStack stack = getStackInSlotRaw(slot);
        if (stack.isEmpty()) {
            owner.device.removeBlockDevice();
        } else {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    (nbt) -> {
                        final CompoundTag tag =
                                ItemStackUtils.getOrCreateModDataTag(nbt)
                                        //? if >=26.1 {
                                        /*.getCompoundOrEmpty(DATA_TAG_NAME);
                                        *///?} else {
                                        .getCompound(DATA_TAG_NAME);
                                        //?}
                        owner.device.updateBlockDevice(tag);
                    });
        }

        // The flash contents are part of the flasher's update tag, so forcing a
        // BlockEntityDataPacket here syncs all tracking clients without a custom message.
        level.sendBlockUpdated(
                owner.getBlockPos(),
                owner.getBlockState(),
                owner.getBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);

        owner.setChanged();
    }

    private void exportDeviceDataToItemStack(final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        var level = owner.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        final CompoundTag tag = new CompoundTag();
        owner.device.exportToItemStack(tag);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                (nbt) -> {
                    ItemStackUtils.getOrCreateModDataTag(nbt).put(DATA_TAG_NAME, tag);
                });
    }
}