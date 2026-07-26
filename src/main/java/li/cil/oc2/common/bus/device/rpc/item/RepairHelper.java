package li.cil.oc2.common.bus.device.rpc.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;

import javax.annotation.Nullable;

class RepairHelper {
    @Nullable
    static Tier getRepairItemTier(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        if (stack.getItem() instanceof final TieredItem tieredItem) {
            return tieredItem.getTier();
        }

        return null;
    }
}
