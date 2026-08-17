package li.cil.oc2.common.item.storage;

import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

public final class MemoryItem extends AbstractStorageItem {
    @Nullable private String descriptionId;

    public MemoryItem(final int defaultCapacity) {
        super(createProperties().stacksTo(4), defaultCapacity);
    }

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId =
                    Util.makeDescriptionId(
                            "item", ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "memory"));
        }
        return descriptionId;
    }
}