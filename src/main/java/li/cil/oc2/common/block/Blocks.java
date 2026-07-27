package li.cil.oc2.common.block;

import li.cil.oc2.api.API;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Blocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(API.MOD_ID);

    public static final DeferredBlock<BusCableBlock> BUS_CABLE =
            BLOCKS.register("bus_cable", BusCableBlock::new);
    public static final DeferredBlock<ChargerBlock> CHARGER =
            BLOCKS.register("charger", ChargerBlock::new);
    public static final DeferredBlock<ComputerBlock> COMPUTER =
            BLOCKS.register("computer", ComputerBlock::new);
    public static final DeferredBlock<MonitorBlock> MONITOR =
            BLOCKS.register("monitor", MonitorBlock::new);
    public static final DeferredBlock<CreativeEnergyBlock> CREATIVE_ENERGY =
            BLOCKS.register("creative_energy", CreativeEnergyBlock::new);
    public static final DeferredBlock<DiskDriveBlock> DISK_DRIVE =
            BLOCKS.register("disk_drive", DiskDriveBlock::new);
    public static final DeferredBlock<FlashMemoryFlasherBlock> FLASH_MEMORY_FLASHER =
            BLOCKS.register("flash_memory_flasher", FlashMemoryFlasherBlock::new);
    public static final DeferredBlock<KeyboardBlock> KEYBOARD =
            BLOCKS.register("keyboard", KeyboardBlock::new);
    public static final DeferredBlock<NetworkConnectorBlock> NETWORK_CONNECTOR =
            BLOCKS.register("network_connector", NetworkConnectorBlock::new);
    public static final DeferredBlock<NetworkHubBlock> NETWORK_HUB =
            BLOCKS.register("network_hub", NetworkHubBlock::new);
    public static final DeferredBlock<NetworkSwitchBlock> NETWORK_SWITCH =
            BLOCKS.register("network_switch", NetworkSwitchBlock::new);
    public static final DeferredBlock<ProjectorBlock> PROJECTOR =
            BLOCKS.register("projector", ProjectorBlock::new);
    public static final DeferredBlock<RedstoneInterfaceBlock> REDSTONE_INTERFACE =
            BLOCKS.register("redstone_interface", RedstoneInterfaceBlock::new);
    public static final DeferredBlock<VxlanBlock> VXLAN_HUB =
            BLOCKS.register("vxlan_hub", VxlanBlock::new);
    public static final DeferredBlock<PciCardCageBlock> PCI_CARD_CAGE =
            BLOCKS.register("pci_card_cage", PciCardCageBlock::new);

    public static final DeferredBlock<InternetGatewayBlock> INTERNET_GATEWAY =
            BLOCKS.register("internet_gateway", InternetGatewayBlock::new);

    public static void initialize(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
