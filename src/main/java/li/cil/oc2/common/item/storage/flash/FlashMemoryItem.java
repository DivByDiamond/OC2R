package li.cil.oc2.common.item.storage.flash;

import javax.annotation.Nullable;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.storage.AbstractStorageItem;
import net.minecraft.Util;

public final class FlashMemoryItem extends AbstractStorageItem {
    @Nullable private String descriptionId;

    public FlashMemoryItem(final int defaultCapacity) {
        super(createProperties().stacksTo(1), defaultCapacity);
    }

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", Items.FLASH_MEMORY.getId());
        }
        return descriptionId;
    }
}