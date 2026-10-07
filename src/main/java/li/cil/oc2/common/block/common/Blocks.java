package li.cil.oc2.common.block.common;

import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.cable.BusCableBlock;
import li.cil.oc2.common.block.computer.ComputerBlock;
import li.cil.oc2.common.block.disk.DiskDriveBlock;
import li.cil.oc2.common.block.disk.FlashMemoryFlasherBlock;
import li.cil.oc2.common.block.energy.ChargerBlock;
import li.cil.oc2.common.block.energy.CreativeEnergyBlock;
import li.cil.oc2.common.block.keyboard.KeyboardBlock;
import li.cil.oc2.common.block.misc.InternetGatewayBlock;
import li.cil.oc2.common.block.misc.PciCardCageBlock;
import li.cil.oc2.common.block.misc.RedstoneInterfaceBlock;
import li.cil.oc2.common.block.misc.SpeakerBlock;
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.block.network.NetworkConnectorBlock;
import li.cil.oc2.common.block.network.NetworkHubBlock;
import li.cil.oc2.common.block.network.NetworkSwitchBlock;
import li.cil.oc2.common.block.network.VxlanBlock;
import li.cil.oc2.common.block.projector.ProjectorBlock;
import li.cil.oc2.platform.BlockHolder;
import li.cil.oc2.platform.Platform;
import net.minecraft.world.level.block.Block;

public final class Blocks {
    public static final BlockHolder<BusCableBlock> BUS_CABLE =
            register("bus_cable", BusCableBlock::new);
    public static final BlockHolder<ChargerBlock> CHARGER =
            register("charger", ChargerBlock::new);
    public static final BlockHolder<ComputerBlock> COMPUTER =
            register("computer", ComputerBlock::new);
    public static final BlockHolder<MonitorBlock> MONITOR =
            register("monitor", MonitorBlock::new);
    public static final BlockHolder<CreativeEnergyBlock> CREATIVE_ENERGY =
            register("creative_energy", CreativeEnergyBlock::new);
    public static final BlockHolder<DiskDriveBlock> DISK_DRIVE =
            register("disk_drive", DiskDriveBlock::new);
    public static final BlockHolder<FlashMemoryFlasherBlock> FLASH_MEMORY_FLASHER =
            register("flash_memory_flasher", FlashMemoryFlasherBlock::new);
    public static final BlockHolder<KeyboardBlock> KEYBOARD =
            register("keyboard", KeyboardBlock::new);
    public static final BlockHolder<NetworkConnectorBlock> NETWORK_CONNECTOR =
            register("network_connector", NetworkConnectorBlock::new);
    public static final BlockHolder<NetworkHubBlock> NETWORK_HUB =
            register("network_hub", NetworkHubBlock::new);
    public static final BlockHolder<NetworkSwitchBlock> NETWORK_SWITCH =
            register("network_switch", NetworkSwitchBlock::new);
    public static final BlockHolder<ProjectorBlock> PROJECTOR =
            register("projector", ProjectorBlock::new);
    public static final BlockHolder<RedstoneInterfaceBlock> REDSTONE_INTERFACE =
            register("redstone_interface", RedstoneInterfaceBlock::new);
    public static final BlockHolder<VxlanBlock> VXLAN_HUB =
            register("vxlan_hub", VxlanBlock::new);
    public static final BlockHolder<PciCardCageBlock> PCI_CARD_CAGE =
            register("pci_card_cage", PciCardCageBlock::new);

    public static final BlockHolder<InternetGatewayBlock> INTERNET_GATEWAY =
            register("internet_gateway", InternetGatewayBlock::new);

    public static final BlockHolder<SpeakerBlock> SPEAKER =
            register("speaker", SpeakerBlock::new);

    private static <B extends Block> BlockHolder<B> register(final String name, final Supplier<B> factory) {
        return Platform.registries().registerBlock(API.MOD_ID, name, factory);
    }

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }
}