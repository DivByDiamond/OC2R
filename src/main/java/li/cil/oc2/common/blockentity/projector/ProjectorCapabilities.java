package li.cil.oc2.common.blockentity.projector;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.projector.ProjectorBlock;
import li.cil.oc2.common.bus.device.vm.block.ProjectorDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
final class ProjectorCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        if (Config.projectorsUseEnergy()) {
            event.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, be, side) ->
                            be instanceof ProjectorBlockEntity self ? self.energy : null,
                    Blocks.PROJECTOR.get());
        }
        event.registerBlock(
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
