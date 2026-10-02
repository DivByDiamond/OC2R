package li.cil.oc2.common.bus.device.vm.item.network;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.bus.device.vm.item.AbstractNetworkInterfaceDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.network.NetworkInterfaceCardItem;
import li.cil.oc2.platform.CapabilityRegistrar;
import net.minecraft.world.item.ItemStack;

public final class NetworkInterfaceCardDevice extends AbstractNetworkInterfaceDevice {
    public NetworkInterfaceCardDevice(final ItemStack identity) {
        super(identity);
    }

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
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