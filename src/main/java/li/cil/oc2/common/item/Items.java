package li.cil.oc2.common.item;

import java.util.function.Function;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.block.ChargerItem;
import li.cil.oc2.common.item.block.ModBlockItem;
import li.cil.oc2.common.item.computer.CPUItem;
import li.cil.oc2.common.item.computer.GPUItem;
import li.cil.oc2.common.item.network.NetworkInterfaceCardItem;
import li.cil.oc2.common.item.network.NetworkTunnelItem;
import li.cil.oc2.common.item.network.cable.BusCableItem;
import li.cil.oc2.common.item.network.cable.BusInterfaceItem;
import li.cil.oc2.common.item.network.cable.NetworkCableItem;
import li.cil.oc2.common.item.storage.FloppyItem;
import li.cil.oc2.common.item.storage.HardDriveItem;
import li.cil.oc2.common.item.storage.MemoryItem;
import li.cil.oc2.common.item.storage.flash.FlashMemoryItem;
import li.cil.oc2.common.item.storage.flash.FlashMemoryWithExternalDataItem;
import li.cil.oc2.common.item.storage.flash.HardDriveWithExternalDataItem;
import li.cil.oc2.common.item.tool.BlockOperationsModule;
import li.cil.oc2.common.item.tool.ManualItem;
import li.cil.oc2.common.item.tool.RobotItem;
import li.cil.oc2.common.item.tool.WrenchItem;
import li.cil.oc2.platform.BlockHolder;
import li.cil.oc2.platform.ItemHolder;
import li.cil.oc2.platform.NeoForgeRegistryBridge;
import li.cil.oc2.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;

@SuppressWarnings("unused")
public final class Items {
    public static final ItemHolder<Item> BUS_CABLE =
            register(Blocks.BUS_CABLE, BusCableItem::new);
    public static final ItemHolder<BusInterfaceItem> BUS_INTERFACE =
            register("bus_interface", BusInterfaceItem::new);
    public static final ItemHolder<Item> CHARGER = register(Blocks.CHARGER, ChargerItem::new);
    public static final ItemHolder<Item> COMPUTER = register(Blocks.COMPUTER);
    public static final ItemHolder<Item> MONITOR = register(Blocks.MONITOR);
    public static final ItemHolder<Item> CREATIVE_ENERGY = register(Blocks.CREATIVE_ENERGY);
    public static final ItemHolder<Item> DISK_DRIVE = register(Blocks.DISK_DRIVE);
    public static final ItemHolder<Item> FLASH_MEMORY_FLASHER =
            register(Blocks.FLASH_MEMORY_FLASHER);
    public static final ItemHolder<Item> KEYBOARD = register(Blocks.KEYBOARD);
    public static final ItemHolder<Item> NETWORK_CONNECTOR = register(Blocks.NETWORK_CONNECTOR);
    public static final ItemHolder<Item> NETWORK_HUB = register(Blocks.NETWORK_HUB);
    // public static final ItemHolder<Item> NETWORK_SWITCH = register(Blocks.NETWORK_SWITCH);
    public static final ItemHolder<Item> PROJECTOR = register(Blocks.PROJECTOR);
    public static final ItemHolder<Item> REDSTONE_INTERFACE = register(Blocks.REDSTONE_INTERFACE);
    public static final ItemHolder<Item> VXLAN_HUB = register(Blocks.VXLAN_HUB);
    public static final ItemHolder<Item> PCI_CARD_CAGE = register(Blocks.PCI_CARD_CAGE);
    public static final ItemHolder<Item> INTERNET_GATEWAY = register(Blocks.INTERNET_GATEWAY);
    public static final ItemHolder<Item> SPEAKER = register(Blocks.SPEAKER);

    public static final ItemHolder<Item> WRENCH = register("wrench", WrenchItem::new);
    public static final ItemHolder<Item> MANUAL = register("manual", ManualItem::new);

    public static final ItemHolder<Item> ROBOT = register("robot", RobotItem::new);
    public static final ItemHolder<NetworkCableItem> NETWORK_CABLE =
            register("network_cable", NetworkCableItem::new);

    public static final ItemHolder<MemoryItem> MEMORY_SMALL =
            register("memory_small", () -> new MemoryItem(2 * Constants.MEGABYTE));
    public static final ItemHolder<MemoryItem> MEMORY_MEDIUM =
            register("memory_medium", () -> new MemoryItem(4 * Constants.MEGABYTE));
    public static final ItemHolder<MemoryItem> MEMORY_LARGE =
            register("memory_large", () -> new MemoryItem(8 * Constants.MEGABYTE));
    public static final ItemHolder<MemoryItem> MEMORY_EXTRA_LARGE =
            register("memory_extra_large", () -> new MemoryItem(16 * Constants.MEGABYTE));

    public static final ItemHolder<HardDriveItem> HARD_DRIVE_SMALL =
            register(
                    "hard_drive_small",
                    () -> new HardDriveItem(Config.diskSizeTier1, DyeColor.LIGHT_GRAY));
    public static final ItemHolder<HardDriveItem> HARD_DRIVE_MEDIUM =
            register(
                    "hard_drive_medium",
                    () -> new HardDriveItem(Config.diskSizeTier2, DyeColor.GREEN));
    public static final ItemHolder<HardDriveItem> HARD_DRIVE_LARGE =
            register(
                    "hard_drive_large",
                    () -> new HardDriveItem(Config.diskSizeTier3, DyeColor.CYAN));
    public static final ItemHolder<HardDriveItem> HARD_DRIVE_EXTRA_LARGE =
            register(
                    "hard_drive_extra_large",
                    () -> new HardDriveItem(Config.diskSizeTier4, DyeColor.YELLOW));

    public static final ItemHolder<HardDriveWithExternalDataItem> HARD_DRIVE_ONYXOS =
            register(
                    "hard_drive_onyxos",
                    () -> new HardDriveWithExternalDataItem(
                            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "onyxos-base"),
                            DyeColor.RED));

    public static final ItemHolder<CPUItem> CPU_TIER_1 =
            register("cpu_tier_1", () -> new CPUItem(Config.cpuFrequencyTier1));
    public static final ItemHolder<CPUItem> CPU_TIER_2 =
            register("cpu_tier_2", () -> new CPUItem(Config.cpuFrequencyTier2));
    public static final ItemHolder<CPUItem> CPU_TIER_3 =
            register("cpu_tier_3", () -> new CPUItem(Config.cpuFrequencyTier3));
    public static final ItemHolder<CPUItem> CPU_TIER_4 =
            register("cpu_tier_4", () -> new CPUItem(Config.cpuFrequencyTier4));
    public static final ItemHolder<CPUItem> CPU_TIER_INF =
            register("cpu_tier_inf", () -> new CPUItem(1_000_000_000));
    public static final ItemHolder<GPUItem> GPU_TIER_1 =
            register("gpu_tier_1", () -> new GPUItem(320, 200, 1));
    public static final ItemHolder<GPUItem> GPU_TIER_2 =
            register("gpu_tier_2", () -> new GPUItem(640, 400, 2));
    public static final ItemHolder<GPUItem> GPU_TIER_3 =
            register("gpu_tier_3", () -> new GPUItem(1024, 768, 3));
    public static final ItemHolder<GPUItem> GPU_TIER_4 =
            register("gpu_tier_4", () -> new GPUItem(1920, 1080, 4));
    public static final ItemHolder<FlashMemoryItem> FLASH_MEMORY_SMALL =
            register(
                    "flash_memory_small",
                    () -> new FlashMemoryItem(Config.flashMemorySizeTier1));
    public static final ItemHolder<FlashMemoryItem> FLASH_MEMORY_MEDIUM =
            register(
                    "flash_memory_medium",
                    () -> new FlashMemoryItem(Config.flashMemorySizeTier2));
    public static final ItemHolder<FlashMemoryItem> FLASH_MEMORY =
            register("flash_memory", () -> new FlashMemoryItem(Config.flashMemorySizeTier3));
    public static final ItemHolder<FlashMemoryWithExternalDataItem> FLASH_MEMORY_CUSTOM =
            register(
                    "flash_memory_custom",
                    () -> new FlashMemoryWithExternalDataItem(FirmwareRegistry.MINUX_ID));
    public static final ItemHolder<FlashMemoryWithExternalDataItem> FLASH_MEMORY_ONYXOS =
            register(
                    "flash_memory_onyxos",
                    () ->
                            new FlashMemoryWithExternalDataItem(
                                    FirmwareRegistry.ONYXOS_ID));

    public static final ItemHolder<FloppyItem> FLOPPY =
            register("floppy", () -> new FloppyItem(512 * Constants.KILOBYTE));
    public static final ItemHolder<FloppyItem> FLOPPY_MODERN =
            register("floppy_modern", () -> new FloppyItem(1440 * Constants.KILOBYTE));

    public static final ItemHolder<Item> REDSTONE_INTERFACE_CARD =
            register("redstone_interface_card");
    public static final ItemHolder<Item> NETWORK_INTERFACE_CARD =
            register("network_interface_card", NetworkInterfaceCardItem::new);
    public static final ItemHolder<Item> NETWORK_TUNNEL_CARD =
            register("network_tunnel_card", NetworkTunnelItem::new);
    public static final ItemHolder<Item> INTERNET_CARD = register("internet_card");
    public static final ItemHolder<Item> FILE_IMPORT_EXPORT_CARD =
            register("file_import_export_card");
    public static final ItemHolder<Item> SOUND_CARD = register("sound_card");

    public static final ItemHolder<Item> INVENTORY_OPERATIONS_MODULE =
            register("inventory_operations_module");
    public static final ItemHolder<Item> BLOCK_OPERATIONS_MODULE =
            register("block_operations_module", BlockOperationsModule::new);
    public static final ItemHolder<Item> NETWORK_TUNNEL_MODULE =
            register("network_tunnel_module", NetworkTunnelItem::new);

    public static final ItemHolder<Item> TRANSISTOR = register("transistor", ModItem::new);
    public static final ItemHolder<Item> SILICON_BLEND = register("silicon_blend", ModItem::new);
    public static final ItemHolder<Item> SILICON = register("silicon", ModItem::new);
    public static final ItemHolder<Item> SILICON_WAFER = register("silicon_wafer", ModItem::new);
    public static final ItemHolder<Item> RAW_SILICON_WAFER =
            register("raw_silicon_wafer", ModItem::new);
    public static final ItemHolder<Item> CIRCUIT_BOARD = register("circuit_board", ModItem::new);

    public static void initialize(IEventBus modBus) {
        Platform.registries().addItemAlias(
                API.MOD_ID,
                ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "flash_memory_buildroot"),
                FLASH_MEMORY_CUSTOM.getId());
        NeoForgeRegistryBridge.instance().bind(modBus);
    }

    private static ItemHolder<Item> register(final String name) {
        return register(name, ModItem::new);
    }

    private static <T extends Item> ItemHolder<T> register(
            final String name, final Supplier<T> factory) {
        return Platform.registries().registerItem(API.MOD_ID, name, factory);
    }

    private static <T extends Block> ItemHolder<Item> register(final BlockHolder<T> block) {
        return register(block, ModBlockItem::new);
    }

    private static <T extends Block, U extends Item> ItemHolder<U> register(
            final BlockHolder<T> block, final Function<T, U> factory) {
        return register(block.getId().getPath(), () -> factory.apply(block.get()));
    }
}