package li.cil.oc2.common.blockentity.monitor;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.block.monitor.MonitorMultiblock;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
final class MonitorCapabilities {
    private MonitorCapabilities() {}

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final MonitorBlockEntity self) {
                        // Only the origin (master) of a multiblock exposes the device group.
                        // Sub-blocks have no devices of their own; their bus connectivity is
                        // provided by the origin's group via the multiblock.
                        if (!MonitorMultiblock.isOrigin(state)) return null;
                        if (side != state.getValue(MonitorBlock.FACING))
                            return self.stateManager.deviceGroup;
                    }
                    return null;
                },
                Blocks.MONITOR.get());
        if (Config.monitorsUseEnergy()) {
            event.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, be, side) -> {
                        if (be instanceof final MonitorBlockEntity self) {
                            if (!MonitorMultiblock.isOrigin(state)) return null;
                            if (side != state.getValue(MonitorBlock.FACING))
                                return self.stateManager.energy;
                        }
                        return null;
                    },
                    Blocks.MONITOR.get());
        }
    }
}
