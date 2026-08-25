package li.cil.oc2.common.energy;

import li.cil.oc2.common.config.Config;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Internal energy buffer of a single bus cable. Capacity and per-tick transfer rate are
 * config-driven; {@link EnergyTransferManager} pulls into and pushes out of these buffers.
 */
public final class CableEnergyStorage extends EnergyStorage {
    public CableEnergyStorage() {
        super(Config.cableEnergyCapacity);
    }

    public int getTransferPerTick() {
        return Config.cableEnergyTransferPerTick;
    }
}