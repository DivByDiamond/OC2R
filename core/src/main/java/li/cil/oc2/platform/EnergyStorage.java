package li.cil.oc2.platform;

/**
 * Loader-independent energy storage contract, matching NeoForge's {@code IEnergyStorage} shape (the
 * de-facto Forge Energy standard other tech mods interoperate with). Implementations of this interface
 * are the ones the mod's own code stores and mutates directly; {@link EnergyCapabilityRegistrar} exposes
 * them to the loader's capability system, and {@link EnergyBridge} wraps whatever a neighboring block,
 * entity or item exposes back into this same shape.
 */
public interface EnergyStorage {
    int receiveEnergy(int maxReceive, boolean simulate);

    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
