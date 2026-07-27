package li.cil.oc2.common.energy;

import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class EnergyStorageItemStack implements IEnergyStorage {
    private final ItemStack stack;
    private final int capacity;
    private final String[] tagPath;

    public EnergyStorageItemStack(
            final ItemStack stack, final int capacity, final String... tagPath) {
        this.stack = stack;
        this.capacity = capacity;
        this.tagPath = tagPath;
    }

    @Override
    public int receiveEnergy(final int maxReceive, final boolean simulate) {
        final int stored = getEnergyStored();
        final int receiveLimit = capacity - stored;
        final int receive = Math.min(maxReceive, receiveLimit);
        if (!simulate) {
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    stack,
                    (tag) -> {
                        NBTUtils.getOrCreateChildTag(tag, tagPath)
                                .putInt(FixedEnergyStorage.STORED_TAG_NAME, stored + receive);
                    });
        }
        return receive;
    }

    @Override
    public int extractEnergy(final int maxExtract, final boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return NBTUtils.getChildTag(tag, tagPath).getInt(FixedEnergyStorage.STORED_TAG_NAME);
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return false; // We don't want our items to be usable as batteries.
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}