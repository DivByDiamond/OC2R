package li.cil.oc2.common.energy;

import li.cil.oc2.common.config.Config;
import net.neoforged.neoforge.energy.EnergyStorage;

public final class CableEnergyStorage extends EnergyStorage {
    public CableEnergyStorage() {
        super(Config.cableEnergyCapacity);
    }

    public int getTransferPerTick() {
        return Config.cableEnergyTransferPerTick;
    }
}