package li.cil.oc2.platform;

import net.minecraft.world.item.ItemStack;

/** Presents several {@link ItemHandler}s as one, slots numbered consecutively. */
public final class CombinedItemHandler implements ItemHandler {
    private final ItemHandler[] handlers;
    private final int[] baseIndex;
    private final int slotCount;

    public CombinedItemHandler(final ItemHandler... handlers) {
        this.handlers = handlers.clone();
        this.baseIndex = new int[handlers.length];
        int index = 0;
        for (int i = 0; i < handlers.length; i++) {
            index += handlers[i].getSlots();
            baseIndex[i] = index;
        }
        this.slotCount = index;
    }

    @Override
    public int getSlots() {
        return slotCount;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        final int handler = handlerIndex(slot);
        return handlers[handler].getStackInSlot(slot - base(handler));
    }

    @Override
    public void setStackInSlot(final int slot, final ItemStack stack) {
        final int handler = handlerIndex(slot);
        handlers[handler].setStackInSlot(slot - base(handler), stack);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        final int handler = handlerIndex(slot);
        return handlers[handler].insertItem(slot - base(handler), stack, simulate);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        final int handler = handlerIndex(slot);
        return handlers[handler].extractItem(slot - base(handler), amount, simulate);
    }

    @Override
    public int getSlotLimit(final int slot) {
        final int handler = handlerIndex(slot);
        return handlers[handler].getSlotLimit(slot - base(handler));
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        final int handler = handlerIndex(slot);
        return handlers[handler].isItemValid(slot - base(handler), stack);
    }

    private int handlerIndex(final int slot) {
        if (slot < 0 || slot >= slotCount) {
            throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + slotCount + ")");
        }
        for (int i = 0; i < baseIndex.length; i++) {
            if (slot < baseIndex[i]) {
                return i;
            }
        }
        throw new IllegalStateException("Unreachable slot " + slot);
    }

    private int base(final int handler) {
        return handler == 0 ? 0 : baseIndex[handler - 1];
    }
}
