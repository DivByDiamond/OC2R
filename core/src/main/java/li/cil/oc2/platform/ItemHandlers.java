package li.cil.oc2.platform;

import net.minecraft.world.item.ItemStack;

/** Static helpers for {@link ItemHandler}, mirroring the NeoForge utility methods we rely on. */
public final class ItemHandlers {
    private ItemHandlers() {}

    /**
     * Inserts {@code stack} into {@code handler}, preferring slots that already hold a matching
     * item before using empty slots. Equivalent to NeoForge's
     * {@code ItemHandlerHelper.insertItemStacked}.
     *
     * @return the remainder that did not fit
     */
    public static ItemStack insertItemStacked(
            final ItemHandler handler, final ItemStack stack, final boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }

        if (!stack.isStackable()) {
            return insertItem(handler, stack, simulate);
        }

        final int slots = handler.getSlots();
        ItemStack remaining = stack;

        // Fill up existing stacks first, then empty slots, mirroring NeoForge's order so
        // insertions land in the same slots as they did before the loader split.
        for (int i = 0; i < slots; i++) {
            final ItemStack slot = handler.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                remaining = handler.insertItem(i, remaining, simulate);
                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        for (int i = 0; i < slots; i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                remaining = handler.insertItem(i, remaining, simulate);
                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return remaining;
    }

    private static ItemStack insertItem(
            final ItemHandler handler, final ItemStack stack, final boolean simulate) {
        ItemStack remaining = stack;
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, simulate);
        }
        return remaining;
    }
}
