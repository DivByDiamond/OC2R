package li.cil.oc2.common.bus.device.provider;

import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.bus.device.provider.block.BlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.ItemHandlerItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.ItemStackCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.energy.EnergyStorageItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.energy.FluidHandlerItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.module.BlockOperationsModuleDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.module.InventoryOperationsModuleDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.module.card.FileImportExportCardItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.module.card.RedstoneInterfaceCardItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.module.card.SoundCardItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.network.InternetCardItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.network.NetworkInterfaceCardItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.network.NetworkTunnelCardItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.network.NetworkTunnelModuleItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.storage.CPUItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.storage.FlashMemoryItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.storage.GPUItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.storage.MemoryItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.storage.disk.FlashMemoryWithExternalDataItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.storage.disk.HardDriveItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.item.storage.disk.HardDriveWithExternalDataItemDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.block.BlockEntityObjectDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.block.BlockStateObjectDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.block.adapter.EnergyStorageBlockDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.block.adapter.FluidHandlerBlockDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.block.adapter.ItemHandlerBlockDeviceProvider;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.Registry;

public final class ProviderRegistry {
    private static final String BLOCK_DEVICE_PROVIDERS =
            //? if >=26.1 {
            /*Registries.BLOCK_DEVICE_PROVIDER.identifier().toString();
            *///?} else {
            Registries.BLOCK_DEVICE_PROVIDER.location().toString();
            //?}
    public static final Registry<BlockDeviceProvider> BLOCK_DEVICE_PROVIDER_REGISTRY =
            Platform.registries().createRegistry(BLOCK_DEVICE_PROVIDERS, API.MOD_ID);

    private static final String ITEM_DEVICE_PROVIDERS =
            //? if >=26.1 {
            /*Registries.ITEM_DEVICE_PROVIDER.identifier().toString();
            *///?} else {
            Registries.ITEM_DEVICE_PROVIDER.location().toString();
            //?}
    public static final Registry<ItemDeviceProvider> ITEM_DEVICE_PROVIDER_REGISTRY =
            Platform.registries().createRegistry(ITEM_DEVICE_PROVIDERS, API.MOD_ID);

    private static void registerItemProvider(
            final String name, final Supplier<? extends ItemDeviceProvider> factory) {
        Platform.registries().register(ITEM_DEVICE_PROVIDERS, API.MOD_ID, name, factory);
    }

    private static void registerBlockProvider(
            final String name, final Supplier<? extends BlockDeviceProvider> factory) {
        Platform.registries().register(BLOCK_DEVICE_PROVIDERS, API.MOD_ID, name, factory);
    }

    public static void initialize() {
        registerItemProvider("memory", MemoryItemDeviceProvider::new);
        registerItemProvider("hard_drive", HardDriveItemDeviceProvider::new);
        registerItemProvider(
                "hard_drive_custom", HardDriveWithExternalDataItemDeviceProvider::new);
        registerItemProvider("flash_memory", FlashMemoryItemDeviceProvider::new);
        registerItemProvider(
                "flash_memory_custom", FlashMemoryWithExternalDataItemDeviceProvider::new);
        registerItemProvider(
                "redstone_interface_card", RedstoneInterfaceCardItemDeviceProvider::new);
        registerItemProvider(
                "file_import_export_card", FileImportExportCardItemDeviceProvider::new);
        registerItemProvider("sound_card", SoundCardItemDeviceProvider::new);
        registerItemProvider("cpu", CPUItemDeviceProvider::new);
        registerItemProvider("gpu", GPUItemDeviceProvider::new);

        registerItemProvider(
                "inventory_operations_module", InventoryOperationsModuleDeviceProvider::new);
        registerItemProvider(
                "block_operations_module", BlockOperationsModuleDeviceProvider::new);
        registerItemProvider(
                "network_tunnel_module", NetworkTunnelModuleItemDeviceProvider::new);

        registerItemProvider(
                "network_interface_card", NetworkInterfaceCardItemDeviceProvider::new);
        registerItemProvider(
                "network_tunnel_card", NetworkTunnelCardItemDeviceProvider::new);
        registerItemProvider("internet_card", InternetCardItemDeviceProvider::new);

        registerItemProvider(
                "item_stack/capability", ItemStackCapabilityDeviceProvider::new);
        registerItemProvider("energy_storage", EnergyStorageItemDeviceProvider::new);
        registerItemProvider("fluid_handler", FluidHandlerItemDeviceProvider::new);
        registerItemProvider("item_handler", ItemHandlerItemDeviceProvider::new);

        registerBlockProvider("block", BlockStateObjectDeviceProvider::new);
        registerBlockProvider("block_entity", BlockEntityObjectDeviceProvider::new);

        registerBlockProvider(
                "block_entity/capability", BlockEntityCapabilityDeviceProvider::new);
        registerBlockProvider("energy_storage", EnergyStorageBlockDeviceProvider::new);
        registerBlockProvider("fluid_handler", FluidHandlerBlockDeviceProvider::new);
        registerBlockProvider("item_handler", ItemHandlerBlockDeviceProvider::new);
    }
}