package li.cil.oc2.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/** {@link EnergyCapabilityRegistrar} wrapping NeoForge's {@link RegisterCapabilitiesEvent}. */
public final class NeoForgeEnergyCapabilityRegistrar implements EnergyCapabilityRegistrar {
    private final RegisterCapabilitiesEvent event;

    // The event is valid only for the duration of the one-shot setup callback that constructs
    // this registrar and is never retained past it, so storing the reference cannot leak mutable
    // state to anything outside that call.
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "event outlives only the setup callback")
    public NeoForgeEnergyCapabilityRegistrar(final RegisterCapabilitiesEvent event) {
        this.event = event;
    }

    @Override
    public void registerBlock(final BlockEnergyProvider provider, final Block... blocks) {
        event.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> unwrap(provider.get(level, pos, state, be, side)),
                blocks);
    }

    @Override
    public void registerEntity(final EntityType<?> type, final EntityEnergyProvider provider) {
        event.registerEntity(
                Capabilities.EnergyStorage.ENTITY,
                type,
                (entity, side) -> unwrap(provider.get(entity, side)));
    }

    @Override
    public void registerItem(final ItemEnergyProvider provider, final ItemLike... items) {
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, unused) -> unwrap(provider.get(stack)),
                items);
    }

    // Package-private for NeoForgeEnergyCapabilityRegistrarTest.
    @Nullable
    static IEnergyStorage unwrap(@Nullable final EnergyStorage storage) {
        if (storage == null) {
            return null;
        }
        return new EnergyStorageWrapper(storage);
    }

    /**
     * {@link IEnergyStorage} view over a {@link EnergyStorage} whose {@link #equals(Object)} and
     * {@link #hashCode()} delegate to the <em>reference identity</em> of the wrapped storage.
     *
     * <p>Capability providers are re-queried on every device scan, and the device bus compares
     * devices through {@code ObjectDevice} → {@code EnergyStorageDevice} (an {@code IdentityProxy})
     * → this wrapper. Two wrappers over the same storage instance must therefore be equal, or the
     * bus would treat the storage as a different device on every scan and re-add it on every
     * neighbor update (observed as a cascading rescan storm hanging a GameTestServer). Wrappers
     * over different storages must never be equal.
     *
     * <p>This deliberately replaces an earlier static identity cache: caching wrappers in an
     * unbounded map leaked one entry per query (item providers build a fresh {@code EnergyStorage}
     * per query, and entries for block/entity storages outlived their owners). Equality needs no
     * cache: block and entity providers return a stable storage instance for their owner's
     * lifetime, which is exactly the scope over which device identity must be stable.
     */
    private static final class EnergyStorageWrapper implements IEnergyStorage {
        private final EnergyStorage storage;

        private EnergyStorageWrapper(final EnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            return storage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            return storage.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return storage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return storage.canExtract();
        }

        @Override
        public boolean canReceive() {
            return storage.canReceive();
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final EnergyStorageWrapper other && other.storage == storage;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(storage);
        }
    }
}
