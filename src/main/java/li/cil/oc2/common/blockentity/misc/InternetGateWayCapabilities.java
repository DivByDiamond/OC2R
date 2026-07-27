package li.cil.oc2.common.blockentity.misc;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.capabilities.Capabilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public class InternetGateWayCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final InternetGateWayBlockEntity self) {
                        return self;
                    }
                    return null;
                },
                Blocks.INTERNET_GATEWAY.get());
        event.registerBlock(
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