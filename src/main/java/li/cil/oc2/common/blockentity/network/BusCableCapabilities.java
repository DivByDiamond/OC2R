package li.cil.oc2.common.blockentity.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.capabilities.Capabilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
final class BusCableCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.DeviceBusElement.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final BusCableBlockEntity self) {
                        if (BusCableStateProperties.getConnectionType(be.getBlockState(), side)
                                != ConnectionType.NONE) {
                            return self.busElement;
                        }
                    }
                    return null;
                },
                li.cil.oc2.common.block.Blocks.BUS_CABLE.get());
    }
}
