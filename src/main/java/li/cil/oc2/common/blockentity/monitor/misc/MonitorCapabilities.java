package li.cil.oc2.common.blockentity.monitor.misc;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.block.monitor.MonitorMultiblock;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = API.MOD_ID)
final class MonitorCapabilities {
    private MonitorCapabilities() {}

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> getDevice(level, pos, state, be, side),
                Blocks.MONITOR.get());
        if (Config.monitorsUseEnergy()) {
            event.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    MonitorCapabilities::getEnergy,
                    Blocks.MONITOR.get());
        }
    }

    @Nullable
    private static Device getDevice(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            final BlockEntity be,
            final Direction side) {
        if (!(be instanceof final MonitorBlockEntity self)) {
            return null;
        }
        // Bus connectivity is available from the rear of every block of a monitor
        // multiblock. Sub-blocks don't own a separate device group; route their
        // capability to the live origin instead. This is important when the bus
        // cable is attached to a non-origin monitor block.
        if (side == state.getValue(MonitorBlock.FACING)) {
            return null;
        }

        final MonitorBlockEntity origin =
                MonitorMultiblock.getOriginEntity(level, pos, state);
        return origin == null ? null : origin.stateManager.deviceGroup;
    }

    @Nullable
    private static IEnergyStorage getEnergy(
            final Level level,
            final BlockPos pos,
            final BlockState state,
            final BlockEntity be,
            final Direction side) {
        if (!(be instanceof MonitorBlockEntity) || side == state.getValue(MonitorBlock.FACING)) {
            return null;
        }
        // Accept energy on any block of a multiblock, but route it into the
        // origin's shared buffer so a cable can charge the screen from any of
        // its blocks. Sub-blocks have no storage of their own.
        final MonitorBlockEntity origin = MonitorMultiblock.getOriginEntity(level, pos, state);
        return origin == null ? null : origin.stateManager.energy;
    }
}
