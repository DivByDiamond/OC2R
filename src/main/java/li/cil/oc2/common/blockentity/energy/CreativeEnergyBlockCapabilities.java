package li.cil.oc2.common.blockentity.energy;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.CapabilityRegistrar;

public final class CreativeEnergyBlockCapabilities {

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
registrar.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final CreativeEnergyBlockEntity self) {
                        return self.energy;
                    }
                    return null;
                },
                Blocks.CREATIVE_ENERGY.get());
    }
}