package li.cil.oc2.common.block.common;

import com.mojang.serialization.MapCodec;
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
import li.cil.oc2.platform.Platform;
import net.minecraft.world.level.block.Block;

public final class BlockCodecs {
    private static final String REGISTRY_ID = "minecraft:block_type";

    private static <T extends Block> Supplier<MapCodec<T>> register(
            final String name, final Supplier<MapCodec<T>> factory) {
        return Platform.registries().register(REGISTRY_ID, API.MOD_ID, name, factory);
    }

    public static final Supplier<MapCodec<BusCableBlock>> BUS_CABLE =
            register("bus_cable", () -> MapCodec.unit(BusCableBlock::new));
    public static final Supplier<MapCodec<ComputerBlock>> COMPUTER =
            register("computer", () -> MapCodec.unit(ComputerBlock::new));
    public static final Supplier<MapCodec<MonitorBlock>> MONITOR =
            register("monitor", () -> MapCodec.unit(MonitorBlock::new));
    public static final Supplier<MapCodec<DiskDriveBlock>> DISK_DRIVE =
            register("disk_drive", () -> MapCodec.unit(DiskDriveBlock::new));
    public static final Supplier<MapCodec<FlashMemoryFlasherBlock>> FLASH_MEMORY_FLASHER =
            register("flash_memory_flasher", () -> MapCodec.unit(FlashMemoryFlasherBlock::new));
    public static final Supplier<MapCodec<KeyboardBlock>> KEYBOARD =
            register("keyboard", () -> MapCodec.unit(KeyboardBlock::new));
    public static final Supplier<MapCodec<NetworkConnectorBlock>> NETWORK_CONNECTOR =
            register("network_connector", () -> MapCodec.unit(NetworkConnectorBlock::new));
    public static final Supplier<MapCodec<ChargerBlock>> CHARGER =
            register("charger", () -> MapCodec.unit(ChargerBlock::new));
    public static final Supplier<MapCodec<CreativeEnergyBlock>> CREATIVE_ENERGY =
            register("creative_energy", () -> MapCodec.unit(CreativeEnergyBlock::new));
    public static final Supplier<MapCodec<NetworkHubBlock>> NETWORK_HUB =
            register("network_hub", () -> MapCodec.unit(NetworkHubBlock::new));
    public static final Supplier<MapCodec<NetworkSwitchBlock>> NETWORK_SWITCH =
            register("network_switch", () -> MapCodec.unit(NetworkSwitchBlock::new));
    public static final Supplier<MapCodec<ProjectorBlock>> PROJECTOR =
            register("projector", () -> MapCodec.unit(ProjectorBlock::new));
    public static final Supplier<MapCodec<RedstoneInterfaceBlock>> REDSTONE_INTERFACE =
            register("redstone_interface", () -> MapCodec.unit(RedstoneInterfaceBlock::new));
    public static final Supplier<MapCodec<VxlanBlock>> VXLAN =
            register("vxlan", () -> MapCodec.unit(VxlanBlock::new));
    public static final Supplier<MapCodec<PciCardCageBlock>> PCI_CARD_CAGE =
            register("pci_card_cage", () -> MapCodec.unit(PciCardCageBlock::new));
    public static final Supplier<MapCodec<InternetGatewayBlock>> INTERNET_GATEWAY =
            register("internet_gateway", () -> MapCodec.unit(InternetGatewayBlock::new));
    public static final Supplier<MapCodec<SpeakerBlock>> SPEAKER =
            register("speaker", () -> MapCodec.unit(SpeakerBlock::new));

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }
}