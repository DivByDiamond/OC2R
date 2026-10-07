package li.cil.oc2.common.blockentity.projector;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.projector.ProjectorBlock;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.platform.CapabilityRegistrar;

public final class ProjectorCapabilities {

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        if (Config.projectorsUseEnergy()) {
registrar.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, be, side) ->
                            be instanceof ProjectorBlockEntity self ? self.energy : null,
                    Blocks.PROJECTOR.get());
        }
        registrar.registerBlock(
                Capabilities.Device.BLOCK,
                (level, pos, state, be, side) -> {
                    if (!(be instanceof ProjectorBlockEntity self)) return null;
                    if (side != self.getBlockState().getValue(ProjectorBlock.FACING).getOpposite())
                        return null;
                    return self.projectorDevice;
                },
                Blocks.PROJECTOR.get());
    }
}
