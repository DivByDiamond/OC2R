package li.cil.oc2.common.energy;

import li.cil.oc2.platform.AbstractEnergyStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class FixedEnergyStorage extends AbstractEnergyStorage {
    public static final String STORED_TAG_NAME = "stored";
    public static final String CAPACITY_TAG_NAME = "capacity";

    public FixedEnergyStorage(final int capacity) {
        super(capacity);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(STORED_TAG_NAME, energy);
        tag.putInt(CAPACITY_TAG_NAME, capacity); // Mostly for tooltips.
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, final Tag tag) {
        if (tag instanceof final CompoundTag compoundTag) {
            //? if >=26.1 {
            /*energy = compoundTag.getIntOr(STORED_TAG_NAME, 0);
            *///?} else {
            energy = compoundTag.getInt(STORED_TAG_NAME);
            //?}
        }
    }
}