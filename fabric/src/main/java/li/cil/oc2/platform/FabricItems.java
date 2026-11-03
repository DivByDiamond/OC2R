package li.cil.oc2.platform;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Foreign Fabric inventories, preserving component variants and sided insertion/extraction rules. */
final class FabricItems implements ItemHandler {
    private final Storage<ItemVariant> storage;

    private FabricItems(final Storage<ItemVariant> storage) { this.storage = storage; }

    @Nullable
    static <T> T findBlock(final BlockCapability<T> capability, final Level level,
                           final BlockPos pos, @Nullable final Direction side) {
        if (capability.getType() != ItemHandler.class) return null;
        final Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
        return storage == null ? null : capability.getType().cast(new FabricItems(storage));
    }

    private List<StorageView<ItemVariant>> slots() {
        final List<StorageView<ItemVariant>> views = new ArrayList<>();
        storage.forEach(views::add);
        return views;
    }

    private StorageView<ItemVariant> slot(final int slot) {
        final List<StorageView<ItemVariant>> views = slots();
        if (slot < 0 || slot >= views.size()) throw new IllegalArgumentException("Invalid item slot");
        return views.get(slot);
    }

    @Override
    public int getSlots() { return slots().size(); }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        final StorageView<ItemVariant> view = slot(slot);
        return view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
    }

    @Override
    public void setStackInSlot(final int slot, final ItemStack stack) {
        throw new UnsupportedOperationException("Foreign inventories only support insert/extract");
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        final StorageView<ItemVariant> view = slot(slot);
        // Slot-specific insertion requires a writable slot, not a combined storage view.
        if (!(view instanceof Storage<?>)) return stack.copy();
        @SuppressWarnings("unchecked")
        final Storage<ItemVariant> destination = (Storage<ItemVariant>) view;
        try (Transaction transaction = Transaction.openOuter()) {
            final long inserted = destination.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            if (!simulate) transaction.commit();
            return stack.copyWithCount(stack.getCount() - (int) inserted);
        }
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        final StorageView<ItemVariant> view = slot(slot);
        final ItemVariant variant = view.getResource();
        if (variant.isBlank()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openOuter()) {
            final long extracted = view.extract(variant, amount, transaction);
            if (!simulate) transaction.commit();
            return variant.toStack((int) extracted);
        }
    }

    @Override
    public int getSlotLimit(final int slot) {
        return (int) Math.min(slot(slot).getCapacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return insertItem(slot, stack, true).getCount() < stack.getCount();
    }
}
