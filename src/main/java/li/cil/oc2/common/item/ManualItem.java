package li.cil.oc2.common.item;

import li.cil.manual.api.ManualModel;
import li.cil.manual.api.ManualScreenStyle;
import li.cil.manual.api.ManualStyle;
import li.cil.manual.api.prefab.item.AbstractManualItem;
import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.client.manual.ModManualScreenStyle;
import li.cil.oc2.client.manual.ModManualStyle;
import li.cil.oc2.common.util.TooltipUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public final class ManualItem extends AbstractManualItem {
    public ManualItem() {
        super(new Item.Properties());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(
            final ItemStack stack,
            final TooltipContext context,
            final List<Component> tooltip,
            final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        TooltipUtils.tryAddDescription(stack, tooltip);
    }

    @Override
    protected ManualModel getManualModel() {
        return Manuals.MANUAL.get();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected ManualStyle getManualStyle() {
        return ModManualStyle.INSTANCE;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected ManualScreenStyle getScreenStyle() {
        return ModManualScreenStyle.INSTANCE;
    }
}
