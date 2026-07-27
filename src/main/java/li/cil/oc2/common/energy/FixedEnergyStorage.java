package li.cil.oc2.common.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

public final class FixedEnergyStorage extends EnergyStorage {
    public static final String STORED_TAG_NAME = "stored";
    public static final String CAPACITY_TAG_NAME = "capacity";

    public FixedEnergyStorage(final int capacity) {
        super(capacity);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(STORED_TAG_NAME, energy);
        tag.putInt(CAPACITY_TAG_NAME, capacity); // Mostly for tooltips.
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, final Tag tag) {
        if (tag instanceof final CompoundTag compoundTag) {
            energy = compoundTag.getInt(STORED_TAG_NAME);
        }
    }
}
