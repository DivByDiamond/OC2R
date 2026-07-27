package li.cil.oc2.common.block.common;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
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
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.block.network.NetworkConnectorBlock;
import li.cil.oc2.common.block.network.NetworkHubBlock;
import li.cil.oc2.common.block.network.NetworkSwitchBlock;
import li.cil.oc2.common.block.network.VxlanBlock;
import li.cil.oc2.common.block.projector.ProjectorBlock;

public final class BlockCodecs {
    public static final DeferredRegister<MapCodec<? extends Block>> BLOCK_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_TYPE, API.MOD_ID);

    public static final Supplier<MapCodec<BusCableBlock>> BUS_CABLE =
            BLOCK_TYPES.register("bus_cable", () -> MapCodec.unit(BusCableBlock::new));
    public static final Supplier<MapCodec<ComputerBlock>> COMPUTER =
            BLOCK_TYPES.register("computer", () -> MapCodec.unit(ComputerBlock::new));
    public static final Supplier<MapCodec<MonitorBlock>> MONITOR =
            BLOCK_TYPES.register("monitor", () -> MapCodec.unit(MonitorBlock::new));
    public static final Supplier<MapCodec<DiskDriveBlock>> DISK_DRIVE =
            BLOCK_TYPES.register("disk_drive", () -> MapCodec.unit(DiskDriveBlock::new));
    public static final Supplier<MapCodec<FlashMemoryFlasherBlock>> FLASH_MEMORY_FLASHER =
            BLOCK_TYPES.register(
                    "flash_memory_flasher", () -> MapCodec.unit(FlashMemoryFlasherBlock::new));
    public static final Supplier<MapCodec<KeyboardBlock>> KEYBOARD =
            BLOCK_TYPES.register("keyboard", () -> MapCodec.unit(KeyboardBlock::new));
    public static final Supplier<MapCodec<NetworkConnectorBlock>> NETWORK_CONNECTOR =
            BLOCK_TYPES.register(
                    "network_connector", () -> MapCodec.unit(NetworkConnectorBlock::new));
    public static final Supplier<MapCodec<ChargerBlock>> CHARGER =
            BLOCK_TYPES.register("charger", () -> MapCodec.unit(ChargerBlock::new));
    public static final Supplier<MapCodec<CreativeEnergyBlock>> CREATIVE_ENERGY =
            BLOCK_TYPES.register("creative_energy", () -> MapCodec.unit(CreativeEnergyBlock::new));
    public static final Supplier<MapCodec<NetworkHubBlock>> NETWORK_HUB =
            BLOCK_TYPES.register("network_hub", () -> MapCodec.unit(NetworkHubBlock::new));
    public static final Supplier<MapCodec<NetworkSwitchBlock>> NETWORK_SWITCH =
            BLOCK_TYPES.register("network_switch", () -> MapCodec.unit(NetworkSwitchBlock::new));
    public static final Supplier<MapCodec<ProjectorBlock>> PROJECTOR =
            BLOCK_TYPES.register("projector", () -> MapCodec.unit(ProjectorBlock::new));
    public static final Supplier<MapCodec<RedstoneInterfaceBlock>> REDSTONE_INTERFACE =
            BLOCK_TYPES.register(
                    "redstone_interface", () -> MapCodec.unit(RedstoneInterfaceBlock::new));
    public static final Supplier<MapCodec<VxlanBlock>> VXLAN =
            BLOCK_TYPES.register("vxlan", () -> MapCodec.unit(VxlanBlock::new));
    public static final Supplier<MapCodec<PciCardCageBlock>> PCI_CARD_CAGE =
            BLOCK_TYPES.register("pci_card_cage", () -> MapCodec.unit(PciCardCageBlock::new));
    public static final Supplier<MapCodec<InternetGatewayBlock>> INTERNET_GATEWAY =
            BLOCK_TYPES.register(
                    "internet_gateway", () -> MapCodec.unit(InternetGatewayBlock::new));

    public static void initialize(IEventBus modBus) {
        BLOCK_TYPES.register(modBus);
    }
}