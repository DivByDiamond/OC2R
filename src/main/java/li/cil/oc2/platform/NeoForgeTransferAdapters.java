package li.cil.oc2.platform;

//? if >=26.1 {
/*import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

 // Adapters between the loader-independent {@link EnergyStorage}, {@link ItemHandler} and {@link
 // FluidHandler} abstractions of this mod (simulate-flag semantics) and NeoForge 26.x's transfer
 // API ({@link EnergyHandler}, {@link ResourceHandler}; transaction semantics).
 //
 // <p>Direction core to NeoForge ("wrappers", used when registering our own providers) has to
 // honour {@link TransactionContext}, which the core interfaces cannot express. Items execute
 // immediately and restore the touched handler from a snapshot on abort (exact, because {@link
 // ItemHandler#setStackInSlot} exists). Energy and fluids have no setter, so they are deferred:
 // the operation is simulated against the storage plus the not yet committed amount of the same
 // transaction, and executed once when the root transaction commits; an aborted transaction never
 // touches the storage. Direction NeoForge to core ("views", used for lookups) opens a root
 // transaction per call and commits it unless the call is a simulation.
 //
 // <p>All wrappers and views compare equal when they wrap the same delegate, for the reason
 // documented in {@code NeoForgeCapabilitiesTest}: the device bus compares devices through these
 // objects.
final class NeoForgeTransferAdapters {
    private NeoForgeTransferAdapters() {}

    // ---------------------------------------------------------------------------------------
    // Energy
    // ---------------------------------------------------------------------------------------

    // Not yet committed net amount per storage, shared by every wrapper over the same storage.
    private static final Map<EnergyStorage, EnergyJournal> ENERGY_JOURNALS = new WeakHashMap<>();

    private static EnergyJournal energyJournal(final EnergyStorage storage) {
        synchronized (ENERGY_JOURNALS) {
            return ENERGY_JOURNALS.computeIfAbsent(storage, EnergyJournal::new);
        }
    }

    private static final class EnergyJournal extends SnapshotJournal<Integer> {
        private final WeakReference<EnergyStorage> storage;
        // Positive: received and not yet applied, negative: extracted and not yet applied.
        private int pending;

        private EnergyJournal(final EnergyStorage storage) {
            this.storage = new WeakReference<>(storage);
        }

        @Override
        protected Integer createSnapshot() {
            return pending;
        }

        @Override
        protected void revertToSnapshot(final Integer snapshot) {
            pending = snapshot;
        }

        @Override
        protected void onRootCommit(final Integer originalState) {
            final int amount = pending;
            pending = 0;
            final EnergyStorage target = storage.get();
            if (target == null) {
                return;
            }
            if (amount > 0) {
                target.receiveEnergy(amount, false);
            } else if (amount < 0) {
                target.extractEnergy(-amount, false);
            }
        }
    }

    // NeoForge {@link EnergyHandler} over a core {@link EnergyStorage}.
    static final class EnergyHandlerWrapper implements EnergyHandler {
        private final EnergyStorage storage;
        private final EnergyJournal journal;

        EnergyHandlerWrapper(final EnergyStorage storage) {
            this.storage = storage;
            this.journal = energyJournal(storage);
        }

        EnergyStorage storage() {
            return storage;
        }

        @Override
        public long getAmountAsLong() {
            return (long) storage.getEnergyStored() + journal.pending;
        }

        @Override
        public long getCapacityAsLong() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public int insert(final int amount, final TransactionContext transaction) {
            if (amount <= 0 || !storage.canReceive()) {
                return 0;
            }
            final int pending = journal.pending;
            // Energy extracted earlier in this transaction can be handed back without asking the
            // storage; only the rest has to fit into what the storage still accepts.
            final int returned = Math.min(amount, Math.max(0, -pending));
            final int rest = amount - returned;
            int accepted = returned;
            if (rest > 0) {
                final int basis = Math.max(0, pending);
                accepted += Math.max(0, Math.min(rest, storage.receiveEnergy(basis + rest, true) - basis));
            }
            if (accepted > 0) {
                journal.updateSnapshots(transaction);
                journal.pending += accepted;
            }
            return accepted;
        }

        @Override
        public int extract(final int amount, final TransactionContext transaction) {
            if (amount <= 0 || !storage.canExtract()) {
                return 0;
            }
            final int pending = journal.pending;
            final int returned = Math.min(amount, Math.max(0, pending));
            final int rest = amount - returned;
            int taken = returned;
            if (rest > 0) {
                final int basis = Math.max(0, -pending);
                taken += Math.max(0, Math.min(rest, storage.extractEnergy(basis + rest, true) - basis));
            }
            if (taken > 0) {
                journal.updateSnapshots(transaction);
                journal.pending -= taken;
            }
            return taken;
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final EnergyHandlerWrapper other && other.storage.equals(storage);
        }

        @Override
        public int hashCode() {
            return storage.hashCode();
        }
    }

     // Core {@link EnergyStorage} over a NeoForge {@link EnergyHandler}. The transfer API has no
     // equivalent of {@code canReceive}/{@code canExtract}, so both report {@code true}; a handler
     // that refuses simply moves zero.
    static final class EnergyStorageView implements EnergyStorage {
        private final EnergyHandler handler;

        EnergyStorageView(final EnergyHandler handler) {
            this.handler = handler;
        }

        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            try (Transaction transaction = Transaction.openRoot()) {
                final int moved = handler.insert(maxReceive, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return moved;
            }
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            try (Transaction transaction = Transaction.openRoot()) {
                final int moved = handler.extract(maxExtract, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return moved;
            }
        }

        @Override
        public int getEnergyStored() {
            return handler.getAmountAsInt();
        }

        @Override
        public int getMaxEnergyStored() {
            return handler.getCapacityAsInt();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final EnergyStorageView other && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Items
    // ---------------------------------------------------------------------------------------

    // One journal per handler, shared by every wrapper over it: wrappers are created per query, and
    // two journals over the same handler would restore their snapshots in the wrong order on abort
    // (the later snapshot already contains the earlier wrapper's changes).
    private static final Map<ItemHandler, ItemJournal> ITEM_JOURNALS = new WeakHashMap<>();

    private static ItemJournal itemJournal(final ItemHandler handler) {
        synchronized (ITEM_JOURNALS) {
            return ITEM_JOURNALS.computeIfAbsent(handler, ItemJournal::new);
        }
    }

    private static final class ItemJournal extends SnapshotJournal<ItemStack[]> {
        private final WeakReference<ItemHandler> handler;

        private ItemJournal(final ItemHandler handler) {
            this.handler = new WeakReference<>(handler);
        }

        @Override
        protected ItemStack[] createSnapshot() {
            final ItemHandler target = handler.get();
            final ItemStack[] slots = new ItemStack[target == null ? 0 : target.getSlots()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = target.getStackInSlot(i).copy();
            }
            return slots;
        }

        @Override
        protected void revertToSnapshot(final ItemStack[] snapshot) {
            final ItemHandler target = handler.get();
            if (target == null) {
                return;
            }
            for (int i = 0; i < snapshot.length; i++) {
                if (!ItemStack.matches(target.getStackInSlot(i), snapshot[i])) {
                    target.setStackInSlot(i, snapshot[i].copy());
                }
            }
        }
    }

    // NeoForge {@code ResourceHandler<ItemResource>} over a core {@link ItemHandler}.
    static final class ItemResourceHandlerWrapper implements ResourceHandler<ItemResource> {
        private final ItemHandler handler;
        private final ItemJournal journal;

        ItemResourceHandlerWrapper(final ItemHandler handler) {
            this.handler = handler;
            this.journal = itemJournal(handler);
        }

        ItemHandler delegate() {
            return handler;
        }

        @Override
        public int size() {
            return handler.getSlots();
        }

        @Override
        public ItemResource getResource(final int index) {
            return ItemResource.of(handler.getStackInSlot(index));
        }

        @Override
        public long getAmountAsLong(final int index) {
            return handler.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacityAsLong(final int index, final ItemResource resource) {
            final int limit = handler.getSlotLimit(index);
            return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
        }

        @Override
        public boolean isValid(final int index, final ItemResource resource) {
            return handler.isItemValid(index, resource.toStack());
        }

        @Override
        public int insert(
                final int index,
                final ItemResource resource,
                final int amount,
                final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            final ItemStack remainder = handler.insertItem(index, resource.toStack(amount), false);
            return amount - remainder.getCount();
        }

        @Override
        public int extract(
                final int index,
                final ItemResource resource,
                final int amount,
                final TransactionContext transaction) {
            if (resource.isEmpty()
                    || amount <= 0
                    || !resource.matches(handler.getStackInSlot(index))) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            return handler.extractItem(index, amount, false).getCount();
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final ItemResourceHandlerWrapper other
                    && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }

    // Core {@link ItemHandler} over a NeoForge {@code ResourceHandler<ItemResource>}.
    static final class ItemHandlerView implements ItemHandler {
        private final ResourceHandler<ItemResource> handler;

        ItemHandlerView(final ResourceHandler<ItemResource> handler) {
            this.handler = handler;
        }

        @Override
        public int getSlots() {
            return handler.size();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return handler.getResource(slot).toStack(handler.getAmountAsInt(slot));
        }

         // The transfer API has no setter, so the slot is replaced by extracting its content and
         // inserting the new stack inside one transaction.
         //
         // @throws UnsupportedOperationException if the handler does not accept the replacement
        @Override
        public void setStackInSlot(final int slot, final ItemStack stack) {
            try (Transaction transaction = Transaction.openRoot()) {
                final ItemResource current = handler.getResource(slot);
                final int amount = handler.getAmountAsInt(slot);
                if (!current.isEmpty() && amount > 0
                        && handler.extract(slot, current, amount, transaction) != amount) {
                    throw new UnsupportedOperationException(
                            "The queried item handler does not support clearing the slot.");
                }
                if (!stack.isEmpty()
                        && handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction)
                                != stack.getCount()) {
                    throw new UnsupportedOperationException(
                            "The queried item handler does not support replacing the slot.");
                }
                transaction.commit();
            }
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                final int inserted =
                        handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return inserted >= stack.getCount()
                        ? ItemStack.EMPTY
                        : stack.copyWithCount(stack.getCount() - inserted);
            }
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            final ItemResource resource = handler.getResource(slot);
            if (resource.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                final int extracted = handler.extract(slot, resource, amount, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return resource.toStack(extracted);
            }
        }

        @Override
        public int getSlotLimit(final int slot) {
            return handler.getCapacityAsInt(slot, handler.getResource(slot));
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return handler.isValid(slot, ItemResource.of(stack));
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final ItemHandlerView other && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Fluids
    // ---------------------------------------------------------------------------------------

    // Operations of a fluid wrapper that wait for the root commit; negative amount drains.
    private static final class FluidJournal extends SnapshotJournal<Integer> {
        private final FluidHandler handler;
        private final List<FluidStack> operations = new ArrayList<>();

        private FluidJournal(final FluidHandler handler) {
            this.handler = handler;
        }

        @Override
        protected Integer createSnapshot() {
            return operations.size();
        }

        @Override
        protected void revertToSnapshot(final Integer snapshot) {
            operations.subList(snapshot, operations.size()).clear();
        }

        @Override
        protected void onRootCommit(final Integer originalState) {
            final List<FluidStack> toApply = new ArrayList<>(operations);
            operations.clear();
            for (final FluidStack operation : toApply) {
                if (operation.getAmount() > 0) {
                    handler.fill(operation, FluidHandler.FluidAction.EXECUTE);
                } else {
                    handler.drain(
                            new FluidStack(operation.getFluid(), -operation.getAmount()),
                            FluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

     // NeoForge {@code ResourceHandler<FluidResource>} over a core {@link FluidHandler}. The core
     // interface fills and drains without a tank index, so index-addressed calls only check that
     // the tank accepts the fluid; and unlike energy there is no overlay of pending operations,
     // a second operation inside one transaction is simulated against the uncommitted storage.
     // (The mod registers no fluid providers, so this direction exists for completeness.)
    static final class FluidResourceHandlerWrapper implements ResourceHandler<FluidResource> {
        private final FluidHandler handler;
        private final FluidJournal journal;

        FluidResourceHandlerWrapper(final FluidHandler handler) {
            this.handler = handler;
            this.journal = new FluidJournal(handler);
        }

        FluidHandler delegate() {
            return handler;
        }

        @Override
        public int size() {
            return handler.getTanks();
        }

        @Override
        public FluidResource getResource(final int index) {
            final FluidStack stack = handler.getFluidInTank(index);
            return stack == null || stack.isEmpty()
                    ? FluidResource.EMPTY
                    : FluidResource.of(stack.getFluid());
        }

        @Override
        public long getAmountAsLong(final int index) {
            final FluidStack stack = handler.getFluidInTank(index);
            return stack == null || stack.isEmpty() ? 0 : stack.getAmount();
        }

        @Override
        public long getCapacityAsLong(final int index, final FluidResource resource) {
            return handler.getTankCapacity(index);
        }

        @Override
        public boolean isValid(final int index, final FluidResource resource) {
            return handler.isFluidValid(index, new FluidStack(resource.getFluid(), 1));
        }

        @Override
        public int insert(
                final int index,
                final FluidResource resource,
                final int amount,
                final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0 || !isValid(index, resource)) {
                return 0;
            }
            final FluidStack stack = new FluidStack(resource.getFluid(), amount);
            final int accepted = handler.fill(stack, FluidHandler.FluidAction.SIMULATE);
            if (accepted > 0) {
                journal.updateSnapshots(transaction);
                journal.operations.add(new FluidStack(resource.getFluid(), accepted));
            }
            return accepted;
        }

        @Override
        public int extract(
                final int index,
                final FluidResource resource,
                final int amount,
                final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0 || resource.getFluid() != getResource(index).getFluid()) {
                return 0;
            }
            final FluidStack drained =
                    handler.drain(
                            new FluidStack(resource.getFluid(), amount),
                            FluidHandler.FluidAction.SIMULATE);
            if (drained == null || drained.isEmpty()) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            journal.operations.add(new FluidStack(resource.getFluid(), -drained.getAmount()));
            return drained.getAmount();
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final FluidResourceHandlerWrapper other
                    && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }

    // Core {@link FluidHandler} over a NeoForge {@code ResourceHandler<FluidResource>}.
    static final class FluidHandlerView implements FluidHandler {
        private final ResourceHandler<FluidResource> handler;

        FluidHandlerView(final ResourceHandler<FluidResource> handler) {
            this.handler = handler;
        }

        @Override
        public int getTanks() {
            return handler.size();
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            final FluidResource resource = handler.getResource(tank);
            return resource.isEmpty()
                    ? FluidStack.EMPTY
                    : new FluidStack(resource.getFluid(), handler.getAmountAsInt(tank));
        }

        @Override
        public int getTankCapacity(final int tank) {
            return handler.getCapacityAsInt(tank, handler.getResource(tank));
        }

        @Override
        public boolean isFluidValid(final int tank, final FluidStack stack) {
            return !stack.isEmpty() && handler.isValid(tank, FluidResource.of(stack.getFluid()));
        }

        @Override
        public int fill(final FluidStack stack, final FluidAction action) {
            if (stack.isEmpty()) {
                return 0;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                final int moved =
                        handler.insert(FluidResource.of(stack.getFluid()), stack.getAmount(), transaction);
                if (action == FluidAction.EXECUTE) {
                    transaction.commit();
                }
                return moved;
            }
        }

        @Override
        public FluidStack drain(final FluidStack stack, final FluidAction action) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }
            return drain(FluidResource.of(stack.getFluid()), stack.getAmount(), action);
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidAction action) {
            for (int tank = 0; tank < handler.size(); tank++) {
                final FluidResource resource = handler.getResource(tank);
                if (!resource.isEmpty() && handler.getAmountAsLong(tank) > 0) {
                    return drain(resource, maxDrain, action);
                }
            }
            return FluidStack.EMPTY;
        }

        private FluidStack drain(
                final FluidResource resource, final int amount, final FluidAction action) {
            try (Transaction transaction = Transaction.openRoot()) {
                final int moved = handler.extract(resource, amount, transaction);
                if (action == FluidAction.EXECUTE) {
                    transaction.commit();
                }
                return moved > 0 ? new FluidStack(resource.getFluid(), moved) : FluidStack.EMPTY;
            }
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            return o instanceof final FluidHandlerView other && other.handler.equals(handler);
        }

        @Override
        public int hashCode() {
            return handler.hashCode();
        }
    }
}
*///?}
