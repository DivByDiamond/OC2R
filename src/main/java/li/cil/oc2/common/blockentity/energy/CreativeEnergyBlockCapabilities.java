package li.cil.oc2.common.blockentity.energy;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.capabilities.Capabilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
final class CreativeEnergyBlockCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(
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