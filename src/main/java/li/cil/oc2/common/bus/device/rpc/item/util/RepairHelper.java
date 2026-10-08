package li.cil.oc2.common.bus.device.rpc.item.util;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
/*import li.cil.oc2.common.config.Tiers;
import net.minecraft.core.component.DataComponents;
*///?} else {
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
//?}

public class RepairHelper {
    @Nullable
    //? if >=26.1 {
    /*public static Tiers getRepairItemTier(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        // Minecraft 26.x has no tiered items; tools carry their tool material's durability.
        if (stack.has(DataComponents.TOOL)) {
            for (final Tiers tier : Tiers.values()) {
                if (tier.getUses() == stack.getMaxDamage()) {
                    return tier;
                }
            }
        }

        return null;
    }
    *///?} else {
    public static Tier getRepairItemTier(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        if (stack.getItem() instanceof final TieredItem tieredItem) {
            return tieredItem.getTier();
        }

        return null;
    }
    //?}
}