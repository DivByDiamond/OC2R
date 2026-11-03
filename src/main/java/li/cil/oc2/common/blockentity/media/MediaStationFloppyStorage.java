package li.cil.oc2.common.blockentity.media;

import li.cil.oc2.common.bus.device.vm.block.disk.DiskDriveContainer;
import li.cil.oc2.common.bus.device.vm.block.disk.DiskDriveDevice;
import li.cil.oc2.common.container.handler.TypedItemStackHandler;
import li.cil.oc2.common.item.block.AbstractBlockDeviceItem;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.common.util.nbt.NBTUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/** One floppy slot, with its own backing handle independent of the inherited flash/HDD slot. */
final class MediaStationFloppyStorage extends TypedItemStackHandler implements DiskDriveContainer {
    final DiskDriveDevice<MediaStationFloppyStorage> device = new DiskDriveDevice<>(this);
    private final MediaStationBlockEntity owner;

    MediaStationFloppyStorage(final MediaStationBlockEntity owner) {
        super(1, ItemTags.DEVICES_FLOPPY);
        this.owner = owner;
    }

    @Override
    public int getSlotLimit(final int slot) { return 1; }

    @Override
    public ItemStack getDiskItemStack() { return super.getStackInSlot(0); }

    @Override
    public void handleDataAccess() {}

    @Override
    protected void onLoad() {
        super.onLoad();
        final ItemStack stack = getDiskItemStack();
        if (!stack.isEmpty()) device.importFromItemStack(data(stack));
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        final ItemStack stack = super.getStackInSlot(slot);
        export(stack);
        return stack;
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (!simulate && amount > 0) export(super.getStackInSlot(slot));
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public CompoundTag serializeNBT(final HolderLookup.Provider registries) {
        export(getDiskItemStack());
        return super.serializeNBT(registries);
    }

    @Override
    protected void onContentsChanged(final int slot) {
        super.onContentsChanged(slot);
        final var level = owner.getLevel();
        if (level == null || level.isClientSide()) return;
        final ItemStack stack = getDiskItemStack();
        if (stack.isEmpty()) device.removeBlockDevice();
        else device.updateBlockDevice(data(stack));
        owner.setChanged();
        level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_CLIENTS);
    }

    private static CompoundTag data(final ItemStack stack) {
        return NBTUtils.getChildTag(ItemStackUtils.getModDataTag(stack), AbstractBlockDeviceItem.DATA_TAG_NAME);
    }

    private void export(final ItemStack stack) {
        final var level = owner.getLevel();
        if (stack.isEmpty() || level == null || level.isClientSide()) return;
        final CompoundTag data = new CompoundTag();
        device.exportToItemStack(data);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> ItemStackUtils.getOrCreateModDataTag(tag).put(AbstractBlockDeviceItem.DATA_TAG_NAME, data));
    }
}
