package li.cil.oc2.common.blockentity.machine;

import li.cil.oc2.common.machine.MachineInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Vanilla menu/hopper access to the same buffer used by device callbacks. */
public interface MachineInventoryPort extends Container {
    MachineInterfaceBlockEntity machine();

    @Override
    default int getContainerSize() { return 18; }

    @Override
    default boolean isEmpty() { return machine().inventory.isEmpty(); }

    @Override
    default ItemStack getItem(final int slot) { return machine().inventory.getStackInSlot(slot); }

    @Override
    default ItemStack removeItem(final int slot, final int count) {
        return machine().inventory.extractItem(slot, count, false);
    }

    @Override
    default ItemStack removeItemNoUpdate(final int slot) {
        return removeItem(slot, Integer.MAX_VALUE);
    }

    @Override
    default void setItem(final int slot, final ItemStack stack) {
        if (slot < 0 || slot >= MachineInventory.RECOVERY) throw new IllegalArgumentException("Invalid buffer slot");
        machine().inventory.setStackInSlot(slot, stack);
    }

    @Override
    default boolean stillValid(final Player player) {
        return machine().isValid() && Container.stillValidBlockEntity(machine(), player);
    }

    @Override
    default void clearContent() {
        for (int slot = 0; slot <= MachineInventory.RECOVERY; slot++) {
            machine().inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
}
