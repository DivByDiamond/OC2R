package li.cil.oc2.common.bus.device.vm.item.network;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.bus.device.vm.item.AbstractNetworkInterfaceDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.network.NetworkInterfaceCardItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class NetworkInterfaceCardDevice extends AbstractNetworkInterfaceDevice {
    public NetworkInterfaceCardDevice(final ItemStack identity) {
        super(identity);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ComputerBlockEntity computer) {
                        NetworkInterfaceCardDevice self =
                                computer.terminalManager.getFirstDevice(NetworkInterfaceCardDevice.class);
                        if (self != null
                                && NetworkInterfaceCardItem.getSideConfiguration(
                                        self.identity, side)) {
                            return self.getNetworkInterface();
                        }
                    }
                    return null;
                },
                Blocks.COMPUTER.get());
    }
}