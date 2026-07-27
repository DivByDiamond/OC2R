package li.cil.oc2.common.bus.device.rpc.item;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

class InventoryHelper {
    static List<ItemEntity> getItemsInRange(final Entity entity) {
        return entity.level()
                .getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2));
    }

    static ItemStack insertStartingAt(
            final IItemHandler handler,
            ItemStack stack,
            final int startSlot,
            final boolean simulate) {
        for (int i = 0; i < handler.getSlots(); i++) {
            final int slot = (startSlot + i) % handler.getSlots();
            stack = handler.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }
}