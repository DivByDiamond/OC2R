package li.cil.oc2.data.model;

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
    public ModItemModelProvider(
            final PackOutput output, final ExistingFileHelper existingFileHelper) {
        super(output, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simple(Items.WRENCH, "item/tools/wrench");
        simple(Items.MANUAL, "item/tools/manual");

        simple(Items.NETWORK_CABLE, "item/components/materials/network_cable");

        simple(Items.MEMORY_SMALL, "item/components/memory/memory_small");
        simple(Items.MEMORY_MEDIUM, "item/components/memory/memory_medium");
        simple(Items.MEMORY_LARGE, "item/components/memory/memory_large");
        simple(Items.MEMORY_EXTRA_LARGE, "item/components/memory/memory_extra_large");
        simple(Items.CPU_TIER_1, "item/components/cpu/cpu_tier_1");
        simple(Items.CPU_TIER_2, "item/components/cpu/cpu_tier_2");
        simple(Items.CPU_TIER_3, "item/components/cpu/cpu_tier_3");
        simple(Items.CPU_TIER_4, "item/components/cpu/cpu_tier_4");
        simple(Items.CPU_TIER_INF, "item/components/cpu/cpu_tier_inf");
        simple(Items.GPU_TIER_1, "item/components/gpu/gpu_tier_1");
        simple(Items.GPU_TIER_2, "item/components/gpu/gpu_tier_2");
        simple(Items.GPU_TIER_3, "item/components/gpu/gpu_tier_3");
        simple(Items.GPU_TIER_4, "item/components/gpu/gpu_tier_4");
        simple(Items.SILICON, "item/components/silicon/silicon");
        simple(Items.SILICON_BLEND, "item/components/silicon/silicon_blend");
        simple(Items.SILICON_WAFER, "item/components/silicon/silicon_wafer");
        simple(Items.RAW_SILICON_WAFER, "item/components/silicon/raw_silicon_wafer");
        simple(Items.HARD_DRIVE_SMALL, "item/storage/hard_drive/hard_drive_base")
                .texture("layer1", "item/storage/hard_drive/hard_drive_tint");
        simple(Items.HARD_DRIVE_MEDIUM, "item/storage/hard_drive/hard_drive_base")
                .texture("layer1", "item/storage/hard_drive/hard_drive_tint");
        simple(Items.HARD_DRIVE_LARGE, "item/storage/hard_drive/hard_drive_base")
                .texture("layer1", "item/storage/hard_drive/hard_drive_tint");
        simple(Items.HARD_DRIVE_EXTRA_LARGE, "item/storage/hard_drive/hard_drive_base")
                .texture("layer1", "item/storage/hard_drive/hard_drive_tint");
        simple(Items.HARD_DRIVE_ONYXOS, "item/storage/hard_drive/hard_drive_base")
                .texture("layer1", "item/storage/hard_drive/hard_drive_slot");
        simple(Items.FLASH_MEMORY_SMALL, "item/storage/flash_memory/flash_memory");
        simple(Items.FLASH_MEMORY_MEDIUM, "item/storage/flash_memory/flash_memory");
        simple(Items.FLASH_MEMORY, "item/storage/flash_memory/flash_memory");
        simple(Items.FLASH_MEMORY_CUSTOM, "item/storage/flash_memory/flash_memory");
        simple(Items.FLOPPY, "item/storage/floppy/floppy_base").texture("layer1", "item/storage/floppy/floppy_tint");
        simple(Items.FLOPPY_MODERN, "item/storage/floppy/floppy_base").texture("layer1", "item/storage/floppy/floppy_tint");

        simple(Items.REDSTONE_INTERFACE_CARD, "item/cards/redstone_interface_card");
        simple(Items.NETWORK_INTERFACE_CARD, "item/cards/network_interface_card");
        simple(Items.INTERNET_CARD, "item/cards/internet_card");
        simple(Items.FILE_IMPORT_EXPORT_CARD, "item/cards/file_import_export_card");
        simple(Items.SOUND_CARD, "item/cards/sound_card");
        simple(Items.NETWORK_TUNNEL_CARD, "item/cards/network_tunnel_card");

        simple(Items.INVENTORY_OPERATIONS_MODULE, "item/modules/inventory_operations_module");
        simple(Items.BLOCK_OPERATIONS_MODULE, "item/modules/block_operations_module");
        simple(Items.NETWORK_TUNNEL_MODULE, "item/modules/network_tunnel_module");

        simple(Items.TRANSISTOR, "item/components/materials/transistor");
        simple(Items.CIRCUIT_BOARD, "item/components/materials/circuit_board");

        withExistingParent(Entities.ROBOT.getId().getPath(), "template_shulker_box");
    }

    private <T extends Item> ItemModelBuilder simple(
            final DeferredItem<T> item, final String texturePath) {
        return singleTexture(
                item.getId().getPath(),
                ResourceLocation.parse("item/generated"),
                "layer0",
                ResourceLocation.fromNamespaceAndPath(API.MOD_ID, texturePath));
    }
}