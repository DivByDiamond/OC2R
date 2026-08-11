package li.cil.oc2.common.bus.device.rpc.item.util;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class InventoryHelper {
    public static List<ItemEntity> getItemsInRange(final Entity entity) {
        return entity.level()
                .getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(2));
    }

    public static ItemStack insertStartingAt(
            final IItemHandler handler,
            final ItemStack stack,
            final int startSlot,
            final boolean simulate) {
        ItemStack remaining = stack;
        for (int i = 0; i < handler.getSlots(); i++) {
            final int slot = (startSlot + i) % handler.getSlots();
            remaining = handler.insertItem(slot, remaining, simulate);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }
}