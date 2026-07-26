
package li.cil.oc2.common.item;

import li.cil.oc2.client.item.CustomItemColors;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.DyedItemColor;

public final class FloppyItem extends AbstractStorageItem {
    public FloppyItem(final int capacity) {
        super(
            new Item.Properties()
                .component(DataComponents.DYED_COLOR, new DyedItemColor(CustomItemColors.BROWN, true)),
            capacity
        );
    }
}
