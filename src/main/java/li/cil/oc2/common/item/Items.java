package li.cil.oc2.common.item;

import java.util.function.Function;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public final class Items {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(API.MOD_ID);

    public static final DeferredItem<Item> BUS_CABLE =
            register(Blocks.BUS_CABLE, BusCableItem::new);
    public static final DeferredItem<BusInterfaceItem> BUS_INTERFACE =
            register("bus_interface", BusInterfaceItem::new);
    public static final DeferredItem<Item> CHARGER = register(Blocks.CHARGER, ChargerItem::new);
    public static final DeferredItem<Item> COMPUTER = register(Blocks.COMPUTER);
    public static final DeferredItem<Item> MONITOR = register(Blocks.MONITOR);
    public static final DeferredItem<Item> CREATIVE_ENERGY = register(Blocks.CREATIVE_ENERGY);
    public static final DeferredItem<Item> DISK_DRIVE = register(Blocks.DISK_DRIVE);
    public static final DeferredItem<Item> FLASH_MEMORY_FLASHER =
            register(Blocks.FLASH_MEMORY_FLASHER);
    public static final DeferredItem<Item> KEYBOARD = register(Blocks.KEYBOARD);
    public static final DeferredItem<Item> NETWORK_CONNECTOR = register(Blocks.NETWORK_CONNECTOR);
    public static final DeferredItem<Item> NETWORK_HUB = register(Blocks.NETWORK_HUB);
    // public static final DeferredItem<Item> NETWORK_SWITCH = register(Blocks.NETWORK_SWITCH);
    public static final DeferredItem<Item> PROJECTOR = register(Blocks.PROJECTOR);
    public static final DeferredItem<Item> REDSTONE_INTERFACE = register(Blocks.REDSTONE_INTERFACE);
    public static final DeferredItem<Item> VXLAN_HUB = register(Blocks.VXLAN_HUB);
    public static final DeferredItem<Item> PCI_CARD_CAGE = register(Blocks.PCI_CARD_CAGE);
    public static final DeferredItem<Item> INTERNET_GATEWAY = register(Blocks.INTERNET_GATEWAY);

    public static final DeferredItem<Item> WRENCH = register("wrench", WrenchItem::new);
    public static final DeferredItem<Item> MANUAL = register("manual", ManualItem::new);

    public static final DeferredItem<Item> ROBOT = register("robot", RobotItem::new);
    public static final DeferredItem<NetworkCableItem> NETWORK_CABLE =
            register("network_cable", NetworkCableItem::new);

    public static final DeferredItem<MemoryItem> MEMORY_SMALL =
            register("memory_small", () -> new MemoryItem(2 * Constants.MEGABYTE));
    public static final DeferredItem<MemoryItem> MEMORY_MEDIUM =
            register("memory_medium", () -> new MemoryItem(4 * Constants.MEGABYTE));
    public static final DeferredItem<MemoryItem> MEMORY_LARGE =
            register("memory_large", () -> new MemoryItem(8 * Constants.MEGABYTE));
    public static final DeferredItem<MemoryItem> MEMORY_EXTRA_LARGE =
            register("memory_extra_large", () -> new MemoryItem(16 * Constants.MEGABYTE));

    public static final DeferredItem<HardDriveItem> HARD_DRIVE_SMALL =
            register(
                    "hard_drive_small",
                    () -> new HardDriveItem(Config.diskSizeFactor, DyeColor.LIGHT_GRAY));
    public static final DeferredItem<HardDriveItem> HARD_DRIVE_MEDIUM =
            register(
                    "hard_drive_medium",
                    () -> new HardDriveItem(2 * Config.diskSizeFactor, DyeColor.GREEN));
    public static final DeferredItem<HardDriveItem> HARD_DRIVE_LARGE =
            register(
                    "hard_drive_large",
                    () -> new HardDriveItem(4 * Config.diskSizeFactor, DyeColor.CYAN));
    public static final DeferredItem<HardDriveItem> HARD_DRIVE_EXTRA_LARGE =
            register(
                    "hard_drive_extra_large",
                    () -> new HardDriveItem(16 * Config.diskSizeFactor, DyeColor.YELLOW));

    public static final DeferredItem<CPUItem> CPU_TIER_1 =
            register("cpu_tier_1", () -> new CPUItem(25_000_000));
    public static final DeferredItem<CPUItem> CPU_TIER_2 =
            register("cpu_tier_2", () -> new CPUItem(50_000_000));
    public static final DeferredItem<CPUItem> CPU_TIER_3 =
            register("cpu_tier_3", () -> new CPUItem(100_000_000));
    public static final DeferredItem<CPUItem> CPU_TIER_4 =
            register("cpu_tier_4", () -> new CPUItem(200_000_000));
    public static final DeferredItem<CPUItem> CPU_TIER_INF =
            register("cpu_tier_inf", () -> new CPUItem(1_000_000_000));
    public static final DeferredItem<FlashMemoryItem> FLASH_MEMORY =
            register("flash_memory", () -> new FlashMemoryItem(12 * Constants.MEGABYTE));
    public static final DeferredItem<FlashMemoryWithExternalDataItem> FLASH_MEMORY_CUSTOM =
            register(
                    "flash_memory_custom",
                    () -> new FlashMemoryWithExternalDataItem(FirmwareRegistry.MINUX.getId()));

    public static final DeferredItem<FloppyItem> FLOPPY =
            register("floppy", () -> new FloppyItem(512 * Constants.KILOBYTE));
    public static final DeferredItem<FloppyItem> FLOPPY_MODERN =
            register("floppy_modern", () -> new FloppyItem(1440 * Constants.KILOBYTE));

    public static final DeferredItem<Item> REDSTONE_INTERFACE_CARD =
            register("redstone_interface_card");
    public static final DeferredItem<Item> NETWORK_INTERFACE_CARD =
            register("network_interface_card", NetworkInterfaceCardItem::new);
    public static final DeferredItem<Item> NETWORK_TUNNEL_CARD =
            register("network_tunnel_card", NetworkTunnelItem::new);
    public static final DeferredItem<Item> INTERNET_CARD = register("internet_card");
    public static final DeferredItem<Item> FILE_IMPORT_EXPORT_CARD =
            register("file_import_export_card");
    public static final DeferredItem<Item> SOUND_CARD = register("sound_card");

    public static final DeferredItem<Item> INVENTORY_OPERATIONS_MODULE =
            register("inventory_operations_module");
    public static final DeferredItem<Item> BLOCK_OPERATIONS_MODULE =
            register("block_operations_module", BlockOperationsModule::new);
    public static final DeferredItem<Item> NETWORK_TUNNEL_MODULE =
            register("network_tunnel_module", NetworkTunnelItem::new);

    public static final DeferredItem<Item> TRANSISTOR = register("transistor", ModItem::new);
    public static final DeferredItem<Item> SILICON_BLEND = register("silicon_blend", ModItem::new);
    public static final DeferredItem<Item> SILICON = register("silicon", ModItem::new);
    public static final DeferredItem<Item> SILICON_WAFER = register("silicon_wafer", ModItem::new);
    public static final DeferredItem<Item> RAW_SILICON_WAFER =
            register("raw_silicon_wafer", ModItem::new);
    public static final DeferredItem<Item> CIRCUIT_BOARD = register("circuit_board", ModItem::new);

    public static void initialize(IEventBus modBus) {
        ITEMS.addAlias(
                ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "flash_memory_buildroot"),
                FLASH_MEMORY_CUSTOM.getId());
        ITEMS.register(modBus);
    }

    private static DeferredItem<Item> register(final String name) {
        return register(name, ModItem::new);
    }

    private static <T extends Item> DeferredItem<T> register(
            final String name, final Supplier<T> factory) {
        return ITEMS.register(name, factory);
    }

    private static <T extends Block> DeferredItem<Item> register(final DeferredBlock<T> block) {
        return register(block, ModBlockItem::new);
    }

    private static <TBlock extends Block, TItem extends Item> DeferredItem<TItem> register(
            final DeferredBlock<TBlock> block, final Function<TBlock, TItem> factory) {
        return register(block.getId().getPath(), () -> factory.apply(block.get()));
    }
}