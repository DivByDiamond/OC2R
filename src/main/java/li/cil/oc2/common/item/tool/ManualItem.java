package li.cil.oc2.common.item.tool;

import li.cil.oc2.client.manual.Manuals;
import li.cil.oc2.client.manual.ModManualScreenStyle;
import li.cil.oc2.client.manual.ModManualStyle;
import li.cil.oc2.common.item.ItemProps;
import li.cil.oc2.manual.api.ManualModel;
import li.cil.oc2.manual.api.ManualScreenStyle;
import li.cil.oc2.manual.api.ManualStyle;
import li.cil.oc2.manual.api.prefab.item.AbstractManualItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class ManualItem extends AbstractManualItem {
    public ManualItem() {
        super(ItemProps.of());
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
