package li.cil.oc2.common.item;

import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.DyedItemColor;

public final class HardDriveWithExternalDataItem extends AbstractBlockDeviceItem {
    @Nullable private String descriptionId;

    public HardDriveWithExternalDataItem(
            final ResourceLocation defaultData, final DyeColor defaultColor) {
        super(
                new Item.Properties()
                        .component(
                                DataComponents.DYED_COLOR,
                                new DyedItemColor(defaultColor.getTextureDiffuseColor(), true)),
                defaultData);
    }

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
}