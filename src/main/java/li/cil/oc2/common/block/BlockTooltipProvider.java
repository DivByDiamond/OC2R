package li.cil.oc2.common.block;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Tooltip hook for blocks. Minecraft 26.x removed {@code Block#appendHoverText}; the block items
 * query this interface instead to keep the tooltips of the blocks that provide one.
 */
public interface BlockTooltipProvider {
    void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag);
}
