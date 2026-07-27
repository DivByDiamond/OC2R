package li.cil.oc2.common.container;

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import li.cil.oc2.common.bus.element.AbstractItemDeviceBusElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractDeviceItemStackHandler extends FixedSizeItemStackHandler {
    private final Supplier<HolderLookup.Provider> providerSupplier;

    public AbstractDeviceItemStackHandler(
            Supplier<HolderLookup.Provider> providerSupplier, final int size) {
        this(providerSupplier, NonNullList.withSize(size, ItemStack.EMPTY));
    }

    public AbstractDeviceItemStackHandler(
            Supplier<HolderLookup.Provider> providerSupplier, final NonNullList<ItemStack> stacks) {
        super(stacks);
        this.providerSupplier = providerSupplier;
    }

    public abstract AbstractItemDeviceBusElement getBusElement();

    public void exportDeviceDataToItemStacks() {
        for (int slot = 0; slot < getSlots(); slot++) {
            getBusElement().exportDeviceDataToItemStack(slot, getStackInSlot(slot));
        }
    }

    @Override
    public final CompoundTag serializeNBT(HolderLookup.Provider provider) {
        throw new UnsupportedOperationException("Use saveItems and saveDevices instead.");
    }

    @Override
    public final void deserializeNBT(HolderLookup.Provider provider, final CompoundTag tag) {
        throw new UnsupportedOperationException("Use loadItems and loadDevices instead.");
    }

    public CompoundTag saveItems(HolderLookup.Provider provider) {
        return super.serializeNBT(provider);
    }

    public CompoundTag saveDevices(HolderLookup.Provider provider) {
        return getBusElement().save(provider);
    }

    public void loadItems(HolderLookup.Provider provider, final CompoundTag tag) {
        super.deserializeNBT(provider, tag);
        for (int slot = 0; slot < getSlots(); slot++) {
            getBusElement().handleSlotContentsChanged(provider, slot, getStackInSlot(slot));
        }
    }

    public void loadDevices(HolderLookup.Provider provider, final CompoundTag tag) {
        getBusElement().loadAdditional(tag, provider);
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(final int slot) {
        final ItemStack stack = super.getStackInSlot(slot);
        getBusElement().exportDeviceDataToItemStack(slot, stack);
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (!simulate && amount > 0) {
            getBusElement().exportDeviceDataToItemStack(slot, super.getStackInSlot(slot));
        }

        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 1;
    }

    @Override
    protected void onContentsChanged(final int slot) {
        super.onContentsChanged(slot);

        getBusElement()
                .handleSlotContentsChanged(providerSupplier.get(), slot, getStackInSlot(slot));
    }
}