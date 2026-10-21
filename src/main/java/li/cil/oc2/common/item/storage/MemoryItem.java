package li.cil.oc2.common.item.storage;

//? if <26.1 {
import javax.annotation.Nullable;
//?}
import li.cil.oc2.api.API;
//? if <26.1 {
import net.minecraft.Util;
//?}
import net.minecraft.resources.ResourceLocation;
//? if >=26.1 {
/*import net.minecraft.util.Util;
*///?}

public final class MemoryItem extends AbstractStorageItem {

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
    @Nullable private String descriptionId;

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