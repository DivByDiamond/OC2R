package li.cil.oc2.common.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

public final class InfiniteEnergyStorage implements IEnergyStorage {
    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public int extractEnergy(final int maxExtract, final boolean simulate) {
        return Math.max(0, maxExtract);
    }

    @Override
    public int receiveEnergy(final int maxReceive, final boolean simulate) {
        return 0;
    }
}