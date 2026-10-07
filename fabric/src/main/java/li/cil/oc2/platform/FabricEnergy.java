package li.cil.oc2.platform;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Adapters between the mod's {@link li.cil.oc2.platform.EnergyStorage} and Team Reborn Energy, the
 * energy standard on Fabric. Units map 1:1 (one FE is one E).
 */
final class FabricEnergy {
    private static final BlockCapability<li.cil.oc2.platform.EnergyStorage> ENERGY_KEY =
            BlockCapability.createSided(
                    FabricCapabilityBridge.standardId("energy"), li.cil.oc2.platform.EnergyStorage.class);

    /** Pending amounts per mod storage; weak so storages of unloaded blocks can be collected. */
    private static final Map<li.cil.oc2.platform.EnergyStorage, Pending> PENDING =
            Collections.synchronizedMap(new WeakHashMap<>());

    private FabricEnergy() {}

    /** Looks up another mod's energy storage next to us, if {@code capability} is the energy key. */
    @Nullable
    @SuppressWarnings("unchecked")
    static <T> T findBlock(
            final BlockCapability<T> capability, final Level level, final BlockPos pos, @Nullable final Direction side) {
        if (!ENERGY_KEY.equals(capability)) {
            return null;
        }
        final EnergyStorage storage = EnergyStorage.SIDED.find(level, pos, side);
        return storage == null ? null : (T) new FromTeamReborn(storage);
    }

    /** Exposes providers of the energy key to other mods through Team Reborn Energy. */
    @SuppressWarnings("unchecked")
    static <T> void exposeBlocks(
            final BlockCapability<T> capability,
            final CapabilityRegistrar.BlockCapabilityProvider<T> provider,
            final Block... blocks) {
        if (!ENERGY_KEY.equals(capability)) {
            return;
        }
        final CapabilityRegistrar.BlockCapabilityProvider<li.cil.oc2.platform.EnergyStorage> energy =
                (CapabilityRegistrar.BlockCapabilityProvider<li.cil.oc2.platform.EnergyStorage>) provider;
        EnergyStorage.SIDED.registerForBlocks((level, pos, state, blockEntity, side) -> {
            final li.cil.oc2.platform.EnergyStorage storage = energy.get(level, pos, state, blockEntity, side);
            return storage == null ? null : new ToTeamReborn(storage);
        }, blocks);
    }

    private static int clamp(final long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    /** Another mod's storage seen through the mod's contract. */
    private record FromTeamReborn(EnergyStorage delegate) implements li.cil.oc2.platform.EnergyStorage {
        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            try (Transaction transaction = Transaction.openOuter()) {
                final long moved = delegate.insert(maxReceive, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return clamp(moved);
            }
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            try (Transaction transaction = Transaction.openOuter()) {
                final long moved = delegate.extract(maxExtract, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return clamp(moved);
            }
        }

        @Override
        public int getEnergyStored() {
            return clamp(delegate.getAmount());
        }

        @Override
        public int getMaxEnergyStored() {
            return clamp(delegate.getCapacity());
        }

        @Override
        public boolean canExtract() {
            return delegate.supportsExtraction();
        }

        @Override
        public boolean canReceive() {
            return delegate.supportsInsertion();
        }

        // Identity comes from the wrapped storage, otherwise the device bus re-adds the device on
        // every scan (see docs/roadmap/multiloader.md, energy migration).
        @Override
        public boolean equals(final Object other) {
            return other instanceof final FromTeamReborn o && delegate.equals(o.delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    /**
     * The mod's storage seen by other mods. Changes are applied when the outermost transaction
     * commits. Until then they are tracked as pending amounts, so several operations inside one
     * transaction see each other's effect instead of all claiming the same free space or charge.
     */
    private static final class ToTeamReborn implements EnergyStorage {
        private final Pending pending;
        private final li.cil.oc2.platform.EnergyStorage delegate;

        ToTeamReborn(final li.cil.oc2.platform.EnergyStorage delegate) {
            this.delegate = delegate;
            this.pending = PENDING.computeIfAbsent(delegate, Pending::new);
        }

        @Override
        public long insert(final long maxAmount, final TransactionContext transaction) {
            final long space = delegate.receiveEnergy(Integer.MAX_VALUE, true) - pending.in;
            final long accepted = Math.max(0, Math.min(maxAmount, space));
            if (accepted > 0) {
                pending.updateSnapshots(transaction);
                pending.in += accepted;
            }
            return accepted;
        }

        @Override
        public long extract(final long maxAmount, final TransactionContext transaction) {
            final long available = delegate.extractEnergy(Integer.MAX_VALUE, true) - pending.out;
            final long removed = Math.max(0, Math.min(maxAmount, available));
            if (removed > 0) {
                pending.updateSnapshots(transaction);
                pending.out += removed;
            }
            return removed;
        }

        @Override
        public long getAmount() {
            return delegate.getEnergyStored();
        }

        @Override
        public long getCapacity() {
            return delegate.getMaxEnergyStored();
        }

        @Override
        public boolean supportsInsertion() {
            return delegate.canReceive();
        }

        @Override
        public boolean supportsExtraction() {
            return delegate.canExtract();
        }
    }

    /** Per-storage amounts inserted and extracted inside transactions that have not committed yet. */
    private static final class Pending extends SnapshotParticipant<long[]> {
        private final li.cil.oc2.platform.EnergyStorage target;
        long in;
        long out;

        Pending(final li.cil.oc2.platform.EnergyStorage target) {
            this.target = target;
        }

        @Override
        protected long[] createSnapshot() {
            return new long[] {in, out};
        }

        @Override
        protected void readSnapshot(final long[] snapshot) {
            in = snapshot[0];
            out = snapshot[1];
        }

        @Override
        protected void onFinalCommit() {
            final long inserted = in;
            final long extracted = out;
            in = 0;
            out = 0;
            if (inserted > 0) {
                target.receiveEnergy(clamp(inserted), false);
            }
            if (extracted > 0) {
                target.extractEnergy(clamp(extracted), false);
            }
        }
    }
}
