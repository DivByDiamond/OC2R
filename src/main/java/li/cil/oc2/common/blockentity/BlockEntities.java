package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.disk.DiskDriveBlockEntity;
import li.cil.oc2.common.blockentity.energy.ChargerBlockEntity;
import li.cil.oc2.common.blockentity.energy.CreativeEnergyBlockEntity;
import li.cil.oc2.common.blockentity.keyboard.KeyboardBlockEntity;
import li.cil.oc2.common.blockentity.misc.FlashMemoryFlasherBlockEntity;
import li.cil.oc2.common.blockentity.misc.InternetGateWayBlockEntity;
import li.cil.oc2.common.blockentity.misc.PciCardCageBlockEntity;
import li.cil.oc2.common.blockentity.misc.RedstoneInterfaceBlockEntity;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.network.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.network.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.network.NetworkHubBlockEntity;
import li.cil.oc2.common.blockentity.network.NetworkSwitchBlockEntity;
import li.cil.oc2.common.blockentity.network.VxlanBlockEntity;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, API.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BusCableBlockEntity>>
            BUS_CABLE = register(Blocks.BUS_CABLE, BusCableBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>>
            CHARGER = register(Blocks.CHARGER, ChargerBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerBlockEntity>>
            COMPUTER = register(Blocks.COMPUTER, ComputerBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MonitorBlockEntity>>
            MONITOR = register(Blocks.MONITOR, MonitorBlockEntity::new);
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<CreativeEnergyBlockEntity>>
            CREATIVE_ENERGY = register(Blocks.CREATIVE_ENERGY, CreativeEnergyBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiskDriveBlockEntity>>
            DISK_DRIVE = register(Blocks.DISK_DRIVE, DiskDriveBlockEntity::new);
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<FlashMemoryFlasherBlockEntity>>
            FLASH_MEMORY_FLASHER =
                    register(Blocks.FLASH_MEMORY_FLASHER, FlashMemoryFlasherBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KeyboardBlockEntity>>
            KEYBOARD = register(Blocks.KEYBOARD, KeyboardBlockEntity::new);
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<NetworkConnectorBlockEntity>>
            NETWORK_CONNECTOR =
                    register(Blocks.NETWORK_CONNECTOR, NetworkConnectorBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkHubBlockEntity>>
            NETWORK_HUB = register(Blocks.NETWORK_HUB, NetworkHubBlockEntity::new);
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<NetworkSwitchBlockEntity>>
            NETWORK_SWITCH = register(Blocks.NETWORK_SWITCH, NetworkSwitchBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProjectorBlockEntity>>
            PROJECTOR = register(Blocks.PROJECTOR, ProjectorBlockEntity::new);
    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<RedstoneInterfaceBlockEntity>>
            REDSTONE_INTERFACE =
                    register(Blocks.REDSTONE_INTERFACE, RedstoneInterfaceBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VxlanBlockEntity>>
            VXLAN_HUB = register(Blocks.VXLAN_HUB, VxlanBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PciCardCageBlockEntity>>
            PCI_CARD_CAGE = register(Blocks.PCI_CARD_CAGE, PciCardCageBlockEntity::new);

    public static final DeferredHolder<
                    BlockEntityType<?>, BlockEntityType<InternetGateWayBlockEntity>>
            INTERNET_GATEWAY = register(Blocks.INTERNET_GATEWAY, InternetGateWayBlockEntity::new);

    public static void initialize(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    @SuppressWarnings("ConstantConditions") // .build(null) is fine
    private static <B extends Block, T extends BlockEntity>
            DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
                    final DeferredBlock<B> block,
                    final BlockEntityType.BlockEntitySupplier<T> factory) {
        return BLOCK_ENTITIES.register(
                block.getId().getPath(),
                () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }
}