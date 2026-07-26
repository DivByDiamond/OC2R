
package li.cil.oc2.data;

import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.item.Items;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(final PackOutput output, final ExistingFileHelper existingFileHelper) {
        super(output, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simple(Items.WRENCH);
        simple(Items.MANUAL);

        simple(Items.NETWORK_CABLE);

        simple(Items.MEMORY_SMALL);
        simple(Items.MEMORY_MEDIUM);
        simple(Items.MEMORY_LARGE);
        simple(Items.MEMORY_EXTRA_LARGE);
        simple(Items.CPU_TIER_1);
        simple(Items.CPU_TIER_2);
        simple(Items.CPU_TIER_3);
        simple(Items.CPU_TIER_4);
        simple(Items.CPU_TIER_INF);
        simple(Items.SILICON);
        simple(Items.SILICON_BLEND);
        simple(Items.SILICON_WAFER);
        simple(Items.RAW_SILICON_WAFER);
        simple(Items.HARD_DRIVE_SMALL, "item/hard_drive_base")
            .texture("layer1", "item/hard_drive_tint");
        simple(Items.HARD_DRIVE_MEDIUM, "item/hard_drive_base")
            .texture("layer1", "item/hard_drive_tint");
        simple(Items.HARD_DRIVE_LARGE, "item/hard_drive_base")
            .texture("layer1", "item/hard_drive_tint");
        simple(Items.HARD_DRIVE_EXTRA_LARGE, "item/hard_drive_base")
            .texture("layer1", "item/hard_drive_tint");
        simple(Items.FLASH_MEMORY);
        simple(Items.FLASH_MEMORY_CUSTOM, "item/flash_memory");
        simple(Items.FLOPPY, "item/floppy_base")
            .texture("layer1", "item/floppy_tint");
        simple(Items.FLOPPY_MODERN, "item/floppy_base")
            .texture("layer1", "item/floppy_tint");

        simple(Items.REDSTONE_INTERFACE_CARD);
        simple(Items.NETWORK_INTERFACE_CARD);
        simple(Items.INTERNET_CARD);
        simple(Items.FILE_IMPORT_EXPORT_CARD);
        simple(Items.SOUND_CARD);
        simple(Items.NETWORK_TUNNEL_CARD);

        simple(Items.INVENTORY_OPERATIONS_MODULE);
        simple(Items.BLOCK_OPERATIONS_MODULE);
        simple(Items.NETWORK_TUNNEL_MODULE);

        simple(Items.TRANSISTOR);
        simple(Items.CIRCUIT_BOARD);

        withExistingParent(Entities.ROBOT.getId().getPath(), "template_shulker_box");
    }

    private <T extends Item> void simple(final DeferredItem<T> item) {
        simple(item, "item/" + item.getId().getPath());
    }

    private <T extends Item> ItemModelBuilder simple(final DeferredItem<T> item, final String texturePath) {
        return singleTexture(item.getId().getPath(),
            ResourceLocation.parse("item/generated"),
            "layer0",
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, texturePath));
    }
}
