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
                (level, pos, state, be, side) -> getDevice(state, be, side),
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
            final BlockState state, final BlockEntity be, final Direction side) {
        if (!(be instanceof final MonitorBlockEntity self)) {
            return null;
        }
        // Only the origin (master) of a multiblock exposes the device group.
        // Sub-blocks have no devices of their own; their bus connectivity is
        // provided by the origin's group via the multiblock.
        if (MonitorMultiblock.isOrigin(state) && side == state.getValue(MonitorBlock.FACING)) {
            return self.stateManager.deviceGroup;
        }
        return null;
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
