package li.cil.oc2.common.item.storage;

//? if <26.1 {
import javax.annotation.Nullable;
//?}
import li.cil.oc2.api.API;
import li.cil.oc2.common.item.ItemProps;
//? if <26.1 {
import net.minecraft.Util;
//?}
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
//? if >=26.1 {
/*import net.minecraft.util.Util;
*///?}
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.DyedItemColor;

public final class HardDriveItem extends AbstractStorageItem {

    public HardDriveItem(final int capacity, final DyeColor defaultColor) {
        super(
                ItemProps.of()
                        .component(
                                DataComponents.DYED_COLOR,
                                //? if >=26.1 {
                                /*new DyedItemColor(defaultColor.getTextureDiffuseColor()))
                        .overrideDescription(
                                Util.makeDescriptionId(
                                        "item", ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "hard_drive"))),
                                *///?} else {
                                new DyedItemColor(defaultColor.getTextureDiffuseColor(), true)),
                                //?}
                capacity);
    }

    //? if <26.1 {
    @Nullable private String descriptionId;

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId =
                    Util.makeDescriptionId(
                            "item",
                            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "hard_drive"));
        }
        return descriptionId;
    }
    //?}
}