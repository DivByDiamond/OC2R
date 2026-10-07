package li.cil.oc2.common.blockentity.network.cable;

import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.CapabilityRegistrar;

public final class BusCableCapabilities {

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
                Capabilities.DeviceBusElement.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final BusCableBlockEntity self
                            && BusCableStateProperties.getConnectionType(be.getBlockState(), side)
                                    != ConnectionType.NONE) {
                        return self.busElement;
                    }
                    return null;
                },
                li.cil.oc2.common.block.common.Blocks.BUS_CABLE.get());
registrar.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final BusCableBlockEntity self) {
                        return self.energy;
                    }
                    return null;
                },
                li.cil.oc2.common.block.common.Blocks.BUS_CABLE.get());
    }
}
