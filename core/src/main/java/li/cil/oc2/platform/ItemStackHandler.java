package li.cil.oc2.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Simple array-backed {@link ItemHandler}, behaviourally identical to NeoForge's
 * {@code ItemStackHandler} (including its NBT format) so existing saves keep loading.
 */
public class ItemStackHandler implements ItemHandler, NbtSerializable<CompoundTag> {
    private static final String SLOT_TAG = "Slot";
    private static final String ITEMS_TAG = "Items";
    private static final String SIZE_TAG = "Size";

    protected NonNullList<ItemStack> stacks;

    public ItemStackHandler() {
        this(1);
    }

    public ItemStackHandler(final int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification =
                    "the handler deliberately operates on the caller's NonNullList; copying would"
                            + " break shared backing stores")
    public ItemStackHandler(final NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    /** Resizes the handler, discarding its contents. */
    public void setSize(final int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void setStackInSlot(final int slot, final ItemStack stack) {
        validateSlotIndex(slot);
        if (ItemStack.matches(stacks.get(slot), stack)) {
            return;
        }
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        validateSlotIndex(slot);
        return stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        validateSlotIndex(slot);

        final ItemStack existing = stacks.get(slot);
        int limit = getStackLimit(slot, stack);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }

        final boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }
        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (amount == 0) {
            return ItemStack.EMPTY;
        }
        validateSlotIndex(slot);

        final ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final int toExtract = Math.min(amount, getStackLimit(slot, existing));
        if (existing.getCount() <= toExtract) {
            if (simulate) {
                return existing.copy();
            }
            stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
            return existing;
        }
        if (!simulate) {
            stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
            onContentsChanged(slot);
        }
        return existing.copyWithCount(toExtract);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 64;
    }

    protected int getStackLimit(final int slot, final ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return true;
    }

    @Override
    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        final ListTag items = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                //? if >=26.1 {
                /*final var ops = provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                final CompoundTag itemTag = new CompoundTag(); // NOPMD: one tag per stored slot
                itemTag.putInt(SLOT_TAG, i);
                itemTag.merge((CompoundTag) ItemStack.CODEC.encodeStart(ops, stacks.get(i)).getOrThrow());
                items.add(itemTag);
                *///?} else {
                final CompoundTag itemTag = new CompoundTag(); // NOPMD: one tag per stored slot
                itemTag.putInt(SLOT_TAG, i);
                items.add(stacks.get(i).save(provider, itemTag));
                //?}
            }
        }
        final CompoundTag tag = new CompoundTag();
        tag.put(ITEMS_TAG, items);
        tag.putInt(SIZE_TAG, stacks.size());
        return tag;
    }

    @Override
    public void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag nbt) {
        //? if >=26.1 {
        /*setSize(nbt.getIntOr(SIZE_TAG, stacks.size()));
        final ListTag items = nbt.getListOrEmpty(ITEMS_TAG);
        *///?} else {
        setSize(nbt.contains(SIZE_TAG, Tag.TAG_INT) ? nbt.getInt(SIZE_TAG) : stacks.size());
        final ListTag items = nbt.getList(ITEMS_TAG, Tag.TAG_COMPOUND);
        //?}
        for (int i = 0; i < items.size(); i++) {
            //? if >=26.1 {
            /*final CompoundTag itemTag = items.getCompoundOrEmpty(i);
            final int slot = itemTag.getIntOr(SLOT_TAG, 0);
            *///?} else {
            final CompoundTag itemTag = items.getCompound(i);
            final int slot = itemTag.getInt(SLOT_TAG);
            //?}
            if (slot >= 0 && slot < stacks.size()) {
                //? if >=26.1 {
                /*ItemStack.CODEC
                        .decode(provider.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), itemTag)
                        .resultOrPartial()
                        .ifPresent(result -> stacks.set(slot, result.getFirst()));
                *///?} else {
                ItemStack.parse(provider, itemTag).ifPresent(stack -> stacks.set(slot, stack));
                //?}
            }
        }
        onLoad();
    }

    protected void validateSlotIndex(final int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            throw new IllegalArgumentException(
                    "Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
        }
    }

    protected void onLoad() {
        // Hook for subclasses.
    }

    protected void onContentsChanged(final int slot) {
        // Hook for subclasses.
    }
}
