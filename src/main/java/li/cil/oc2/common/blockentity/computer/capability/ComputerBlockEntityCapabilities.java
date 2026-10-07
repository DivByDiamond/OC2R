package li.cil.oc2.common.blockentity.computer.capability;

import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.platform.CapabilityRegistrar;

public final class ComputerBlockEntityCapabilities {
    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ComputerBlockEntity self) {
                        return self.deviceItems.combinedItemHandlers;
                    }
                    return null;
                },
                Blocks.COMPUTER.get());
        registrar.registerBlock(
                Capabilities.DeviceBusElement.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ComputerBlockEntity self) {
                        return self.busElement;
                    }
                    return null;
                },
                Blocks.COMPUTER.get());
        registrar.registerBlock(
                Capabilities.TerminalUserProvider.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final ComputerBlockEntity self) {
                        return self.terminalManager;
                    }
                    return null;
                },
                Blocks.COMPUTER.get());
        if (Config.computersUseEnergy()) {
registrar.registerBlock(
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