package li.cil.oc2.common.config.common.energy;

import net.neoforged.neoforge.common.ModConfigSpec;

public class EnergySpec {
    // BLOCKS //
    public final ModConfigSpec.DoubleValue busCableEnergyPerTick;
    public final ModConfigSpec.DoubleValue busInterfaceEnergyPerTick;
    public final ModConfigSpec.IntValue computerEnergyPerTick;
    public final ModConfigSpec.IntValue computerEnergyStorage;
    public final ModConfigSpec.IntValue chargerEnergyPerTick;
    public final ModConfigSpec.IntValue chargerEnergyStorage;
    public final ModConfigSpec.IntValue projectorEnergyPerTick;
    public final ModConfigSpec.IntValue projectorEnergyStorage;
    public final ModConfigSpec.IntValue monitorEnergyPerTick;
    public final ModConfigSpec.IntValue monitorEnergyStorage;
    public final ModConfigSpec.IntValue cardCageEnergyPerTick;
    public final ModConfigSpec.IntValue cardCageEnergyStorage;
    public final ModConfigSpec.IntValue gatewayEnergyPerPacket;
    public final ModConfigSpec.IntValue gatewayEnergyStorage;
    // ENTITIES //
    public final ModConfigSpec.IntValue robotEnergyPerTick;
    public final ModConfigSpec.IntValue robotEnergyStorage;
    // ITEMS //
    public final ModConfigSpec.DoubleValue memoryEnergyPerMegabytePerTick;
    public final ModConfigSpec.DoubleValue hardDriveEnergyPerMegabytePerTick;
    public final ModConfigSpec.DoubleValue cpuEnergyPerMegahertzPerTick;
    public final ModConfigSpec.IntValue redstoneInterfaceCardEnergyPerTick;
    public final ModConfigSpec.IntValue networkInterfaceEnergyPerTick;
    public final ModConfigSpec.IntValue fileImportExportCardEnergyPerTick;
    public final ModConfigSpec.IntValue soundCardEnergyPerTick;
    public final ModConfigSpec.IntValue blockOperationsModuleEnergyPerTick;
    public final ModConfigSpec.IntValue inventoryOperationsModuleEnergyPerTick;
    public final ModConfigSpec.IntValue networkTunnelEnergyPerTick;
    public final ModConfigSpec.IntValue gpuEnergyPerTickTier1;
    public final ModConfigSpec.IntValue gpuEnergyPerTickTier2;
    public final ModConfigSpec.IntValue gpuEnergyPerTickTier3;
    public final ModConfigSpec.IntValue gpuEnergyPerTickTier4;

    public EnergySpec(ModConfigSpec.Builder builder) {
        builder.push("blocks");

        busCableEnergyPerTick =
                builder.comment("The amount of energy consumed per tick by a bus cable")
                        .defineInRange("busCableEnergyPerTick", 0.05, 0, Double.MAX_VALUE);

        busInterfaceEnergyPerTick =
                builder.comment("The amount of energy consumed per tick by a bus interface")
                        .defineInRange("busInterfaceEnergyPerTick", 0.05, 0, Double.MAX_VALUE);

        computerEnergyPerTick =
                builder.comment("The amount of energy consumed per tick by a computer")
                        .defineInRange("computerEnergyPerTick", 10, 0, Integer.MAX_VALUE);

        computerEnergyStorage =
                builder.comment("The amount of energy stored in a computer")
                        .defineInRange("computerEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        chargerEnergyPerTick =
                builder.comment("The amount of energy consumed per tick by a charger")
                        .defineInRange("chargerEnergyPerTick", 2500, 0, Integer.MAX_VALUE);

        chargerEnergyStorage =
                builder.comment("The amount of energy stored in a charger")
                        .defineInRange("chargerEnergyStorage", 10000, 0, Integer.MAX_VALUE);

        projectorEnergyPerTick =
                builder.comment("The amount of energy consumed per tick by a projector")
                        .defineInRange("projectorEnergyPerTick", 20, 0, Integer.MAX_VALUE);

        projectorEnergyStorage =
                builder.comment("The amount of energy stored in a projector")
                        .defineInRange("projectorEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        monitorEnergyPerTick =
                builder.comment("The amount of energy consumed per tick by a monitor")
                        .defineInRange("monitorEnergyPerTick", 15, 0, Integer.MAX_VALUE);

        monitorEnergyStorage =
                builder.comment("The amount of energy stored in a monitor")
                        .defineInRange("monitorEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        cardCageEnergyPerTick =
                builder.comment("The amount of energy consumed per tick by a card cage")
                        .defineInRange("cardCageEnergyPerTick", 20, 0, Integer.MAX_VALUE);

        cardCageEnergyStorage =
                builder.comment("The amount of energy stored in a card cage")
                        .defineInRange("cardCageEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        gatewayEnergyPerPacket =
                builder.comment("The amount of energy consumed per packet by a gateway")
                        .defineInRange("gatewayEnergyPerPacket", 20, 0, Integer.MAX_VALUE);

        gatewayEnergyStorage =
                builder.comment("The amount of energy stored in a gateway")
                        .defineInRange("gatewayEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("entities");

        robotEnergyPerTick =
                builder.comment("The amount of energy consumed per tick by a robot")
                        .defineInRange("gatewayEnergyStorage", 5, 0, Integer.MAX_VALUE);

        robotEnergyStorage =
                builder.comment("The amount of energy stored in a robot")
                        .defineInRange("robotEnergyStorage", 750000, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("items");

        memoryEnergyPerMegabytePerTick =
                builder.comment(
                                "The amount of energy consumed per megabyte per tick for memory"
                                        + " modules")
                        .defineInRange("memoryEnergyPerMegabytePerTick", 0.05, 0, Double.MAX_VALUE);

        hardDriveEnergyPerMegabytePerTick =
                builder.comment(
                                "The amount of energy consumed per megabyte per tick for hard drive"
                                        + " modules")
                        .defineInRange(
                                "hardDriveEnergyPerMegabytePerTick", 1.0, 0, Double.MAX_VALUE);

        cpuEnergyPerMegahertzPerTick =
                builder.comment(
                                "The amount of energy consumed per megahertz per tick for CPU"
                                        + " modules")
                        .defineInRange("cpuEnergyPerMegahertzPerTick", 0.1, 0, Double.MAX_VALUE);

        gpuEnergyPerTickTier1 =
                builder.comment("The amount of energy consumed per tick for GPU Tier 1 modules")
                        .defineInRange("gpuEnergyPerTickTier1", 2, 0, Integer.MAX_VALUE);

        gpuEnergyPerTickTier2 =
                builder.comment("The amount of energy consumed per tick for GPU Tier 2 modules")
                        .defineInRange("gpuEnergyPerTickTier2", 3, 0, Integer.MAX_VALUE);

        gpuEnergyPerTickTier3 =
                builder.comment("The amount of energy consumed per tick for GPU Tier 3 modules")
                        .defineInRange("gpuEnergyPerTickTier3", 5, 0, Integer.MAX_VALUE);

        gpuEnergyPerTickTier4 =
                builder.comment("The amount of energy consumed per tick for GPU Tier 4 modules")
                        .defineInRange("gpuEnergyPerTickTier4", 8, 0, Integer.MAX_VALUE);

        redstoneInterfaceCardEnergyPerTick =
                builder.comment(
                                "The amount of energy consumed per tick for redstone interface"
                                        + " cards")
                        .defineInRange(
                                "redstoneInterfaceCardEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        networkInterfaceEnergyPerTick =
                builder.comment(
                                "The amount of energy consumed per tick for network interface"
                                        + " cards")
                        .defineInRange(
                                "redstoneInterfaceCardEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        fileImportExportCardEnergyPerTick =
                builder.comment(
                                "The amount of energy consumed per tick for file import/export"
                                        + " cards")
                        .defineInRange(
                                "fileImportExportCardEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        soundCardEnergyPerTick =
                builder.comment("The amount of energy consumed per tick for sound cards")
                        .defineInRange("soundCardEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        blockOperationsModuleEnergyPerTick =
                builder.comment(
                                "The amount of energy consumed per tick for block operations"
                                        + " modules")
                        .defineInRange(
                                "blockOperationsModuleEnergyPerTick", 2, 0, Integer.MAX_VALUE);

        inventoryOperationsModuleEnergyPerTick =
                builder.comment(
                                "The amount of energy consumed per tick for inventory operations"
                                        + " modules")
                        .defineInRange(
                                "inventoryOperationsModuleEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        networkTunnelEnergyPerTick =
                builder.comment("The amount of energy consumed per tick for network tunnels")
                        .defineInRange("networkTunnelEnergyPerTick", 2, 0, Integer.MAX_VALUE);

        builder.pop();
    }

    public void loadValues() {
        EnergySpecValues.loadValues(this);
    }
}