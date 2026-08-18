package li.cil.oc2.common.container;

import javax.annotation.Nullable;
import li.cil.oc2.common.container.slot.LockedSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractContainer extends AbstractContainerMenu {
    protected static final int HOTBAR_SIZE = 9;
    protected static final int SLOT_SIZE = 18;
    protected static final int PLAYER_INVENTORY_ROWS = 3;
    protected static final int PLAYER_INVENTORY_COLUMNS = 9;
    protected static final int PLAYER_INVENTORY_HOTBAR_SPACING = 4;

    public AbstractContainer(@Nullable final MenuType<?> type, final int id) {
        super(type, id);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot from = slots.get(index);
        final ItemStack stack = from.getItem().copy();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final boolean intoPlayerInventory = !from.container.equals(player.getInventory());
        final ItemStack fromStack = from.getItem();

        final int step = intoPlayerInventory ? -1 : 1;
        final int begin = intoPlayerInventory ? slots.size() - 1 : 0;

        if (fromStack.getMaxStackSize() > 1) {
            stackIntoExistingSlots(from, fromStack, stack, step, begin);
        }
        moveIntoEmptySlots(from, fromStack, step, begin);

        return from.getItem().getCount() < stack.getCount() ? from.getItem() : ItemStack.EMPTY;
    }

    private void stackIntoExistingSlots(
            final Slot from,
            final ItemStack fromStack,
            final ItemStack stack,
            final int step,
            final int begin) {
        for (int i = begin; i >= 0 && i < slots.size(); i += step) {
            final Slot into = slots.get(i);
            if (stackIntoSlot(from, fromStack, stack, into)) {
                return;
            }
        }
    }

    private boolean stackIntoSlot(
            final Slot from, final ItemStack fromStack, final ItemStack stack, final Slot into) {
        if (into.container.equals(from.container)) {
            return false;
        }
        if (!into.mayPlace(fromStack)) {
            return false;
        }
        if (!into.hasItem()) {
            return false;
        }
        final ItemStack intoStack = into.getItem();
        if (!ItemStack.matches(fromStack, intoStack)) {
            return false;
        }
        final int maxSizeInSlot =
                Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(stack));
        final int spaceInSlot = maxSizeInSlot - intoStack.getCount();
        if (spaceInSlot <= 0) {
            return false;
        }
        final int itemsMoved = Math.min(spaceInSlot, fromStack.getCount());
        if (itemsMoved <= 0) {
            return false;
        }
        intoStack.grow(from.remove(itemsMoved).getCount());
        into.setChanged();
        return from.getItem().isEmpty();
    }

    private void moveIntoEmptySlots(
            final Slot from, final ItemStack fromStack, final int step, final int begin) {
        for (int i = begin; i >= 0 && i < slots.size(); i += step) {
            if (from.getItem().isEmpty()) {
                break;
            }

            final Slot into = slots.get(i);
            if (into.container.equals(from.container)) {
                continue;
            }

            if (!into.mayPlace(fromStack)) {
                continue;
            }

            if (into.hasItem()) {
                continue;
            }

            final int maxSizeInSlot =
                    Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(fromStack));
            final int itemsMoved = Math.min(maxSizeInSlot, fromStack.getCount());
            into.set(from.remove(itemsMoved));
        }
    }

    protected int createPlayerInventoryAndHotbarSlots(
            final Inventory inventory, final int startX, final int startY) {
        final int nextIndex =
                createHotbarSlots(
                        inventory,
                        0,
                        startX,
                        startY
                                + PLAYER_INVENTORY_ROWS * SLOT_SIZE
                                + PLAYER_INVENTORY_HOTBAR_SPACING);
        return createPlayerInventorySlots(inventory, nextIndex, startX, startY);
    }

    protected int createPlayerInventorySlots(
            final Inventory inventory, final int startIndex, final int startX, final int startY) {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; ++row) {
            for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; ++column) {
                final int index = startIndex + row * PLAYER_INVENTORY_COLUMNS + column;
                final int x = startX + column * SLOT_SIZE;
                final int y = startY + row * SLOT_SIZE;

                final Slot slot;
                if (isSlotLocked(inventory, index)) {
                    slot = new LockedSlot(inventory, index, x, y); // NOPMD per-slot data
                } else {
                    slot = new Slot(inventory, index, x, y); // NOPMD per-slot data
                }

                this.addSlot(slot);
            }
        }

        return startIndex + PLAYER_INVENTORY_ROWS * PLAYER_INVENTORY_COLUMNS;
    }

    protected int createHotbarSlots(
            final Inventory inventory, final int startIndex, final int startX, final int startY) {
        for (int i = 0; i < HOTBAR_SIZE; ++i) {
            final int index = startIndex + i;
            final int x = startX + i * SLOT_SIZE;

            final Slot slot;
            if (isSlotLocked(inventory, index)) {
                slot = new LockedSlot(inventory, index, x, startY); // NOPMD per-slot data
            } else {
                slot = new Slot(inventory, index, x, startY); // NOPMD per-slot data
            }

            this.addSlot(slot);
        }

        return startIndex + HOTBAR_SIZE;
    }

    protected boolean isSlotLocked(final Inventory inventory, final int slot) {
        return false;
    }
}