package li.cil.oc2.common.blockentity.disk;

import li.cil.oc2.common.bus.device.vm.block.DiskDriveDevice;
import li.cil.oc2.common.container.TypedItemStackHandler;
import li.cil.oc2.common.item.AbstractBlockDeviceItem;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.DiskDriveFloppyMessage;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nonnull;

final class DiskDriveItemStackHandler extends TypedItemStackHandler {
    private final DiskDriveBlockEntity blockEntity;

    DiskDriveItemStackHandler(final DiskDriveBlockEntity blockEntity) {
        super(1, ItemTags.DEVICES_FLOPPY);
        this.blockEntity = blockEntity;
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
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        exportDeviceDataToItemStack(getStackInSlotRaw(0));
        return super.serializeNBT(provider);
    }

    @Override
    protected void onContentsChanged(final int slot) {
        super.onContentsChanged(slot);

        final var level = blockEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        final ItemStack stack = getStackInSlotRaw(slot);
        if (stack.isEmpty()) {
            blockEntity.device.removeBlockDevice();
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, (nbt) -> {
                final CompoundTag tag = ItemStackUtils.getOrCreateModDataTag(nbt).getCompound(AbstractBlockDeviceItem.DATA_TAG_NAME);
                blockEntity.device.updateBlockDevice(tag);
            });
        }

        Network.sendToClientsTrackingBlockEntity(new DiskDriveFloppyMessage(blockEntity), blockEntity);

        blockEntity.setChanged();
    }

    private void exportDeviceDataToItemStack(final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        final var level = blockEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        final CompoundTag tag = new CompoundTag();
        blockEntity.device.exportToItemStack(tag);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (nbt) -> {
            ItemStackUtils.getOrCreateModDataTag(nbt).put(AbstractBlockDeviceItem.DATA_TAG_NAME, tag);
        });
    }
}
