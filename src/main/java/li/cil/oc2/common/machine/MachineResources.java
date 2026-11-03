package li.cil.oc2.common.machine;

import li.cil.oc2.platform.EnergyStorage;
import li.cil.oc2.platform.FluidHandler;
import li.cil.oc2.platform.FluidStack;

/** Device-owned buffers; foreign handlers are never given a reference to mutable state. */
public final class MachineResources {
    public static final int FLUID_CAPACITY = 8000;
    public static final int ENERGY_CAPACITY = 10000;
    private final Runnable changed;
    private FluidStack storedFluid = FluidStack.EMPTY;
    private FluidStack pendingFluid = FluidStack.EMPTY;
    private int storedEnergy;
    private boolean transferring;

    public MachineResources(final Runnable changed) { this.changed = changed; }

    public FluidStack fluid() { return storedFluid; }

    public FluidStack recovery() { return pendingFluid; }

    public int energy() { return storedEnergy; }

    /** Restores persisted state without trusting counts or capacities from disk. */
    public void restore(final FluidStack contents, final FluidStack pending, final int stored) {
        storedFluid = contents.withAmount(Math.clamp(contents.getAmount(), 0, FLUID_CAPACITY));
        pendingFluid = pending.withAmount(Math.clamp(pending.getAmount(), 0, 1000));
        storedEnergy = Math.clamp(stored, 0, ENERGY_CAPACITY);
    }

    public int moveEnergy(final EnergyStorage target, final int requested, final boolean pull) {
        begin(requested);
        try {
            final int moved;
            if (pull) {
                moved = target.extractEnergy(Math.min(requested, ENERGY_CAPACITY - storedEnergy), false);
                storedEnergy += moved;
            } else {
                moved = target.receiveEnergy(Math.min(requested, storedEnergy), false);
                storedEnergy -= moved;
            }
            changed.run();
            return moved;
        } finally {
            transferring = false;
        }
    }

    public int moveFluid(final FluidHandler target, final int requested, final boolean pull) {
        begin(requested);
        try {
            if (!pendingFluid.isEmpty()) {
                throw new IllegalStateException("Recover pending fluid before another transfer");
            }
            if (pull) {
                final int room = FLUID_CAPACITY - storedFluid.getAmount();
                if (room <= 0) return 0;
                final int limit = Math.min(requested, room);
                final FluidStack preview = storedFluid.isEmpty()
                        ? target.drain(limit, FluidHandler.FluidAction.SIMULATE)
                        : target.drain(storedFluid.withAmount(limit), FluidHandler.FluidAction.SIMULATE);
                if (preview.isEmpty() || (!storedFluid.isEmpty() && !storedFluid.sameResource(preview))) return 0;
                pendingFluid = target.drain(preview.withAmount(Math.min(requested, room)),
                        FluidHandler.FluidAction.EXECUTE);
                changed.run();
                return recoverFluidInternal();
            }
            if (storedFluid.isEmpty()) return 0;
            final int accepted = target.fill(storedFluid.withAmount(Math.min(requested, storedFluid.getAmount())),
                    FluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) return 0;
            pendingFluid = storedFluid.withAmount(Math.min(accepted, Math.min(requested, storedFluid.getAmount())));
            storedFluid = storedFluid.withAmount(storedFluid.getAmount() - pendingFluid.getAmount());
            changed.run();
            final int moved = target.fill(pendingFluid, FluidHandler.FluidAction.EXECUTE);
            pendingFluid = pendingFluid.withAmount(pendingFluid.getAmount() - moved);
            changed.run();
            return moved;
        } finally {
            transferring = false;
        }
    }

    public int recoverFluid() {
        begin(1);
        try {
            return recoverFluidInternal();
        } finally {
            transferring = false;
        }
    }

    private int recoverFluidInternal() {
        if (pendingFluid.isEmpty() || (!storedFluid.isEmpty() && !storedFluid.sameResource(pendingFluid))) return 0;
        final int moved = Math.min(pendingFluid.getAmount(), FLUID_CAPACITY - storedFluid.getAmount());
        storedFluid = pendingFluid.withAmount(storedFluid.getAmount() + moved);
        pendingFluid = pendingFluid.withAmount(pendingFluid.getAmount() - moved);
        changed.run();
        return moved;
    }

    private void begin(final int requested) {
        if (transferring) throw new IllegalStateException("Resource transfer is already in progress");
        if (requested < 1 || requested > 1000) throw new IllegalArgumentException("Amount must be 1..1000");
        transferring = true;
    }
}
