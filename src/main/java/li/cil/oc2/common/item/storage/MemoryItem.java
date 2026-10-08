package li.cil.oc2.common.item.storage;

import javax.annotation.Nullable;
import li.cil.oc2.api.API;
//? if >=26.1 {
/*import net.minecraft.util.Util;
*///?} else {
import net.minecraft.Util;
//?}
import net.minecraft.resources.ResourceLocation;

public final class MemoryItem extends AbstractStorageItem {
    @Nullable private String descriptionId;

    public MemoryItem(final int defaultCapacity) {
        //? if >=26.1 {
        /*super(
                createProperties()
                        .stacksTo(4)
                        .overrideDescription(
                                Util.makeDescriptionId(
                                        "item", ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "memory"))),
                defaultCapacity);
        *///?} else {
        super(createProperties().stacksTo(4), defaultCapacity);
        //?}
    }

    //? if <26.1 {
    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId =
                    Util.makeDescriptionId(
                            "item", ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "memory"));
        }
        return descriptionId;
    }
    //?}
}