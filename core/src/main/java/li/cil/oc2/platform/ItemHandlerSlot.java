package li.cil.oc2.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A menu {@link Slot} backed by one slot of an {@link ItemHandler}. */
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification =
                "the slot wraps the live ItemHandler it serves; exposing it is how menu code"
                        + " reaches the handler it edits")
public class ItemHandlerSlot extends Slot {
    private static final Container EMPTY_CONTAINER = new SimpleContainer(0);

    private final ItemHandler itemHandler;
    private final int index;

    public ItemHandlerSlot(
            final ItemHandler itemHandler, final int index, final int xPosition, final int yPosition) {
        super(EMPTY_CONTAINER, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    public ItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return !stack.isEmpty() && itemHandler.isItemValid(index, stack);
    }

    @Override
    public ItemStack getItem() {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public void set(final ItemStack stack) {
        itemHandler.setStackInSlot(index, stack);
        setChanged();
    }

    @Override
    public void onQuickCraft(final ItemStack oldStack, final ItemStack newStack) {
        // Nothing to track.
    }

    @Override
    public int getMaxStackSize() {
        return itemHandler.getSlotLimit(index);
    }

    @Override
    public int getMaxStackSize(final ItemStack stack) {
        final ItemStack maxAdd = stack.copy();
        final int maxInput = stack.getMaxStackSize();
        maxAdd.setCount(maxInput);

        final ItemStack current = itemHandler.getStackInSlot(index);
        itemHandler.setStackInSlot(index, ItemStack.EMPTY);
        final ItemStack remainder = itemHandler.insertItem(index, maxAdd, true);
        itemHandler.setStackInSlot(index, current);
        return maxInput - remainder.getCount();
    }

    @Override
    public boolean mayPickup(final Player player) {
        return !itemHandler.extractItem(index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(final int amount) {
        return itemHandler.extractItem(index, amount, false);
    }
}
