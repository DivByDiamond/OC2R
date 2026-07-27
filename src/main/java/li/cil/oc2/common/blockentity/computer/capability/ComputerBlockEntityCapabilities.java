package li.cil.oc2.common.blockentity.computer.capability;

import li.cil.oc2.api.API;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
public final class ComputerBlockEntityCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ComputerBlockEntity self) {
                        return self.deviceItems.combinedItemHandlers;
                    }
                    return null;
                },
                Blocks.COMPUTER.get());
        event.registerBlock(
                Capabilities.DeviceBusElement.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ComputerBlockEntity self) {
                        return self.busElement;
                    }
                    return null;
                },
                Blocks.COMPUTER.get());
        event.registerBlock(
                Capabilities.TerminalUserProvider.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ComputerBlockEntity self) {
                        return self;
                    }
                    return null;
                },
                Blocks.COMPUTER.get());
        if (Config.computersUseEnergy()) {
            event.registerBlock(
                    Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, be, side) -> {
                        if (be instanceof final ComputerBlockEntity self) {
                            return self.energy;
                        }
                        return null;
                    },
                    Blocks.COMPUTER.get());
        }
    }
}
