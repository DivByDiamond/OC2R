package li.cil.oc2.common.energy;

import li.cil.oc2.common.config.Config;
import li.cil.oc2.platform.AbstractEnergyStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Internal energy buffer of a single bus cable. Capacity and per-tick transfer rate are
 * config-driven; {@link EnergyTransferManager} pulls into and pushes out of these buffers.
 * The buffer is saved with the cable's block entity (docs/CABLE-SYSTEM.md, "Energy"), so
 * stored energy survives chunk unloads and world restarts.
 */
public final class CableEnergyStorage extends AbstractEnergyStorage {
    private static final String STORED_TAG_NAME = "stored";

    public CableEnergyStorage() {
        super(Config.cableEnergyCapacity);
    }

    public int getTransferPerTick() {
        return Config.cableEnergyTransferPerTick;
    }

    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(STORED_TAG_NAME, energy);
        return tag;
    }

    public void deserializeNBT(final HolderLookup.Provider provider, final Tag tag) {
        if (tag instanceof final CompoundTag compoundTag) {
            //? if >=26.1 {
            /*energy = compoundTag.getIntOr(STORED_TAG_NAME, 0);
            *///?} else {
            energy = compoundTag.getInt(STORED_TAG_NAME);
            //?}
        }
    }
}
