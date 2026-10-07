package li.cil.oc2.common.blockentity.misc.gateway;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.CapabilityRegistrar;

public class InternetGateWayCapabilities {
    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final InternetGateWayBlockEntity self) {
                        return self;
                    }
                    return null;
                },
                Blocks.INTERNET_GATEWAY.get());
registrar.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final InternetGateWayBlockEntity self) {
                        return self.energy;
                    }
                    return null;
                },
                Blocks.INTERNET_GATEWAY.get());
    }
}