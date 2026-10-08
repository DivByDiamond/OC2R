package li.cil.oc2.common.item.storage.flash;

import javax.annotation.Nullable;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.storage.AbstractStorageItem;
//? if >=26.1 {
/*import net.minecraft.util.Util;
*///?} else {
import net.minecraft.Util;
//?}

public final class FlashMemoryItem extends AbstractStorageItem {
    @Nullable private String descriptionId;

    public FlashMemoryItem(final int defaultCapacity) {
        //? if >=26.1 {
        /*super(
                createProperties()
                        .stacksTo(1)
                        .overrideDescription(Util.makeDescriptionId("item", Items.FLASH_MEMORY.getId())),
                defaultCapacity);
        *///?} else {
        super(createProperties().stacksTo(1), defaultCapacity);
        //?}
    }

    //? if <26.1 {
    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", Items.FLASH_MEMORY.getId());
        }
        return descriptionId;
    }
    //?}
}