package li.cil.oc2.common.machine;

import java.util.function.Consumer;
import li.cil.oc2.common.container.handler.FixedSizeItemStackHandler;
import li.cil.oc2.platform.ItemHandler;
import net.minecraft.world.item.ItemStack;

/** Eighteen normal slots and a persistent, insertion-protected recovery slot. */
public final class MachineInventory extends FixedSizeItemStackHandler {
    public static final int RECOVERY = 18;
    private final Runnable changed;
    private boolean transferring;
    private Consumer<ItemStack> removal;

    public MachineInventory(final Runnable changed) {
        super(19);
        this.changed = changed;
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return removal == null && slot != RECOVERY;
    }

    @Override
    protected void onContentsChanged(final int slot) {
        changed.run();
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (removal != null || (slot == RECOVERY && transferring)) return ItemStack.EMPTY;
        return super.extractItem(slot, amount, simulate);
    }

    /** Extract first; material rejected by the destination remains owned by this inventory. */
    @SuppressWarnings("PMD.UnusedAssignment") // Reentrant foreign handlers observe this field through other methods.
    public int transfer(final ItemHandler source, final int from, final ItemHandler destination,
                        final int to, final int requested) {
        if (transferring || removal != null || !getStackInSlot(RECOVERY).isEmpty()) {
            throw new IllegalStateException("Recover pending items before another transfer");
        }
        if (from < 0 || from >= source.getSlots() || to < 0 || to >= destination.getSlots()) {
            throw new IllegalArgumentException("Invalid item slot (slots start at zero)");
        }
        if (requested < 1 || requested > 64) throw new IllegalArgumentException("Amount must be 1..64");
        transferring = true;
        try {
            final ItemStack preview = source.extractItem(from, requested, true).copy();
            if (preview.isEmpty() || removal != null) return 0;
            final int accepted = preview.getCount() - destination.insertItem(to, preview.copy(), true).getCount();
            if (accepted <= 0 || removal != null) return 0;
            final ItemStack taken = source.extractItem(from, Math.min(accepted, requested), false).copy();
            // Escrow before invoking foreign code: even an exception cannot destroy extracted items.
            setStackInSlot(RECOVERY, taken);
            if (taken.isEmpty() || removal != null) return 0;
            final ItemStack remainder = destination.insertItem(to, taken.copy(), false);
            setStackInSlot(RECOVERY, remainder.copy());
            return taken.getCount() - remainder.getCount();
        } finally {
            transferring = false;
            finishRemoval();
        }
    }

    /** Defer drops until an in-flight foreign insertion has returned its actual remainder. */
    public void close(final Consumer<ItemStack> drop) {
        removal = drop;
        if (!transferring) finishRemoval();
    }

    private void finishRemoval() {
        if (removal == null) return;
        for (int slot = 0; slot < getSlots(); slot++) {
            final ItemStack stack = super.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) removal.accept(stack);
        }
    }

    /** Moves rejected items back into normal buffer slots; external automation can also extract slot 18. */
    public int recover() {
        if (transferring) throw new IllegalStateException("Transfer is in progress");
        final ItemStack pending = getStackInSlot(RECOVERY).copy();
        final int before = pending.getCount();
        setStackInSlot(RECOVERY, ItemStack.EMPTY);
        ItemStack left = pending;
        for (int slot = 0; slot < RECOVERY && !left.isEmpty(); slot++) left = insertItem(slot, left, false);
        setStackInSlot(RECOVERY, left);
        return before - left.getCount();
    }
}
