package li.cil.oc2.common.config.common.energy;

import li.cil.oc2.common.config.Config;

final class EnergySpecValues {
    static void loadValues(final EnergySpec spec) {
        // BLOCKS //
        Config.busCableEnergyPerTick = spec.busCableEnergyPerTick.get();
        Config.busInterfaceEnergyPerTick = spec.busInterfaceEnergyPerTick.get();
        Config.cableEnergyCapacity = spec.cableEnergyCapacity.get();
        Config.cableEnergyTransferPerTick = spec.cableEnergyTransferPerTick.get();
        Config.computerEnergyPerTick = spec.computerEnergyPerTick.get();
        Config.computerEnergyStorage = spec.computerEnergyStorage.get();
        Config.chargerEnergyPerTick = spec.chargerEnergyPerTick.get();
        Config.chargerEnergyStorage = spec.chargerEnergyStorage.get();
        Config.projectorEnergyPerTick = spec.projectorEnergyPerTick.get();
        Config.projectorEnergyStorage = spec.projectorEnergyStorage.get();
        Config.monitorEnergyPerTick = spec.monitorEnergyPerTick.get();
        Config.monitorEnergyStorage = spec.monitorEnergyStorage.get();
        Config.cardCageEnergyPerTick = spec.cardCageEnergyPerTick.get();
        Config.cardCageEnergyStorage = spec.cardCageEnergyStorage.get();
        Config.gatewayEnergyPerPacket = spec.gatewayEnergyPerPacket.get();
        Config.gatewayEnergyStorage = spec.gatewayEnergyStorage.get();
        // ENTITIES //
        Config.robotEnergyPerTick = spec.robotEnergyPerTick.get();
        Config.robotEnergyStorage = spec.robotEnergyStorage.get();
        // ITEMS //
        Config.memoryEnergyPerMegabytePerTick = spec.memoryEnergyPerMegabytePerTick.get();
        Config.hardDriveEnergyPerMegabytePerTick = spec.hardDriveEnergyPerMegabytePerTick.get();
        Config.cpuEnergyPerMegahertzPerTick = spec.cpuEnergyPerMegahertzPerTick.get();
        Config.redstoneInterfaceCardEnergyPerTick = spec.redstoneInterfaceCardEnergyPerTick.get();
        Config.networkInterfaceEnergyPerTick = spec.networkInterfaceEnergyPerTick.get();
        Config.fileImportExportCardEnergyPerTick = spec.fileImportExportCardEnergyPerTick.get();
        Config.soundCardEnergyPerTick = spec.soundCardEnergyPerTick.get();
        Config.blockOperationsModuleEnergyPerTick = spec.blockOperationsModuleEnergyPerTick.get();
        Config.inventoryOperationsModuleEnergyPerTick =
                spec.inventoryOperationsModuleEnergyPerTick.get();
        Config.gpuEnergyPerTickTier1 = spec.gpuEnergyPerTickTier1.get();
        Config.gpuEnergyPerTickTier2 = spec.gpuEnergyPerTickTier2.get();
        Config.gpuEnergyPerTickTier3 = spec.gpuEnergyPerTickTier3.get();
        Config.gpuEnergyPerTickTier4 = spec.gpuEnergyPerTickTier4.get();
    }
}
