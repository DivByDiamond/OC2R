package li.cil.oc2.common.item.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.DyedItemColor;

public final class FloppyItem extends AbstractStorageItem {
    /** Matches the client-side {@code CustomItemColors.BROWN} tint. */
    private static final int DEFAULT_COLOR = 0xFF745C42;

    public FloppyItem(final int capacity) {
        super(
                new Item.Properties()
                        .component(
                                DataComponents.DYED_COLOR,
                                new DyedItemColor(DEFAULT_COLOR, true)),
                capacity);
    }
}