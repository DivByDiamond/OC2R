package li.cil.oc2.common.blockentity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.disk.DiskDriveBlockEntity;
import li.cil.oc2.common.blockentity.energy.ChargerBlockEntity;
import li.cil.oc2.common.blockentity.energy.CreativeEnergyBlockEntity;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.blockentity.misc.PciCardCageBlockEntity;
import li.cil.oc2.common.blockentity.misc.SpeakerBlockEntity;
import li.cil.oc2.common.blockentity.misc.flash.FlashMemoryFlasherBlockEntity;
import li.cil.oc2.common.blockentity.misc.gateway.InternetGateWayBlockEntity;
import li.cil.oc2.common.blockentity.misc.redstone.RedstoneInterfaceBlockEntity;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.network.cable.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.hub.NetworkHubBlockEntity;
import li.cil.oc2.common.blockentity.network.switches.NetworkSwitchBlockEntity;
import li.cil.oc2.common.blockentity.network.vxlan.VxlanBlockEntity;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.platform.BlockHolder;
import li.cil.oc2.platform.NeoForgeRegistryBridge;
import li.cil.oc2.platform.Platform;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Builder.build(Type) accepts null; vanilla passes null for the datafixer type")
public final class BlockEntities {
    public static final Supplier<BlockEntityType<BusCableBlockEntity>>
            BUS_CABLE = register(Blocks.BUS_CABLE, BusCableBlockEntity::new);
    public static final Supplier<BlockEntityType<ChargerBlockEntity>>
            CHARGER = register(Blocks.CHARGER, ChargerBlockEntity::new);
    public static final Supplier<BlockEntityType<ComputerBlockEntity>>
            COMPUTER = register(Blocks.COMPUTER, ComputerBlockEntity::new);
    public static final Supplier<BlockEntityType<MonitorBlockEntity>>
            MONITOR = register(Blocks.MONITOR, MonitorBlockEntity::new);
    public static final Supplier<BlockEntityType<CreativeEnergyBlockEntity>>
            CREATIVE_ENERGY = register(Blocks.CREATIVE_ENERGY, CreativeEnergyBlockEntity::new);
    public static final Supplier<BlockEntityType<DiskDriveBlockEntity>>
            DISK_DRIVE = register(Blocks.DISK_DRIVE, DiskDriveBlockEntity::new);
    public static final Supplier<BlockEntityType<FlashMemoryFlasherBlockEntity>>
            FLASH_MEMORY_FLASHER =
                    register(Blocks.FLASH_MEMORY_FLASHER, FlashMemoryFlasherBlockEntity::new);
    public static final Supplier<BlockEntityType<KeyboardBlockEntity>>
            KEYBOARD = register(Blocks.KEYBOARD, KeyboardBlockEntity::new);
    public static final Supplier<BlockEntityType<NetworkConnectorBlockEntity>>
            NETWORK_CONNECTOR =
                    register(Blocks.NETWORK_CONNECTOR, NetworkConnectorBlockEntity::new);
    public static final Supplier<BlockEntityType<NetworkHubBlockEntity>>
            NETWORK_HUB = register(Blocks.NETWORK_HUB, NetworkHubBlockEntity::new);
    public static final Supplier<BlockEntityType<NetworkSwitchBlockEntity>>
            NETWORK_SWITCH = register(Blocks.NETWORK_SWITCH, NetworkSwitchBlockEntity::new);
    public static final Supplier<BlockEntityType<ProjectorBlockEntity>>
            PROJECTOR = register(Blocks.PROJECTOR, ProjectorBlockEntity::new);
    public static final Supplier<BlockEntityType<RedstoneInterfaceBlockEntity>>
            REDSTONE_INTERFACE =
                    register(Blocks.REDSTONE_INTERFACE, RedstoneInterfaceBlockEntity::new);
    public static final Supplier<BlockEntityType<VxlanBlockEntity>>
            VXLAN_HUB = register(Blocks.VXLAN_HUB, VxlanBlockEntity::new);
    public static final Supplier<BlockEntityType<PciCardCageBlockEntity>>
            PCI_CARD_CAGE = register(Blocks.PCI_CARD_CAGE, PciCardCageBlockEntity::new);

    public static final Supplier<BlockEntityType<InternetGateWayBlockEntity>>
            INTERNET_GATEWAY = register(Blocks.INTERNET_GATEWAY, InternetGateWayBlockEntity::new);

    public static final Supplier<BlockEntityType<SpeakerBlockEntity>>
            SPEAKER = register(Blocks.SPEAKER, SpeakerBlockEntity::new);

    public static void initialize(IEventBus modBus) {
        NeoForgeRegistryBridge.instance().bind(modBus);
    }

    @SuppressWarnings("ConstantConditions") // .build(null) is fine
    private static <B extends Block, T extends BlockEntity>
            Supplier<BlockEntityType<T>> register(
                    final BlockHolder<B> block,
                    final BlockEntityType.BlockEntitySupplier<T> factory) {
        return Platform.registries().register("minecraft:block_entity_type", API.MOD_ID, 
                block.getId().getPath(),
                () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }
}