package li.cil.oc2.platform;

/**
 * Reference {@link EnergyStorage} implementation with clamped receive/extract, mirroring NeoForge's
 * own {@code EnergyStorage} reference class so existing storage subclasses keep the same behavior
 * after moving off the NeoForge-specific base.
 */
public class AbstractEnergyStorage implements EnergyStorage {
    protected int energy;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public AbstractEnergyStorage(final int capacity) {
        this(capacity, capacity, capacity, 0);
    }

    public AbstractEnergyStorage(final int capacity, final int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer, 0);
    }

    public AbstractEnergyStorage(final int capacity, final int maxReceive, final int maxExtract) {
        this(capacity, maxReceive, maxExtract, 0);
    }

    public AbstractEnergyStorage(
            final int capacity, final int maxReceive, final int maxExtract, final int energy) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        if (!canReceive() || toReceive <= 0) {
            return 0;
        }

        final int energyReceived =
                Math.max(0, Math.min(this.capacity - this.energy, Math.min(this.maxReceive, toReceive)));
        if (!simulate) {
            this.energy += energyReceived;
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(final int toExtract, final boolean simulate) {
        if (!canExtract() || toExtract <= 0) {
            return 0;
        }

        final int energyExtracted = Math.min(this.energy, Math.min(this.maxExtract, toExtract));
        if (!simulate) {
            this.energy -= energyExtracted;
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored() {
        return this.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return this.capacity;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0;
    }
}
