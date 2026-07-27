package li.cil.oc2.common.blockentity.misc;

import static li.cil.oc2.common.item.AbstractBlockDeviceItem.DATA_TAG_NAME;

import javax.annotation.Nonnull;
import li.cil.oc2.common.container.TypedItemStackHandler;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.FirmwareFlasherMessage;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.ItemStackUtils;
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
    @Nonnull
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (slot == 0 && !simulate && amount > 0) {
            exportDeviceDataToItemStack(getStackInSlotRaw(0));
        }

        return super.extractItem(slot, amount, simulate);
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
                                        .getCompound(DATA_TAG_NAME);
                        owner.device.updateBlockDevice(tag);
                    });
        }

        Network.sendToClientsTrackingBlockEntity(new FirmwareFlasherMessage(owner), owner);

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