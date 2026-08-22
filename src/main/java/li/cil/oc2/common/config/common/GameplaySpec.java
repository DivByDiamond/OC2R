package li.cil.oc2.common.config.common;

import li.cil.oc2.common.config.Config;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.ModConfigSpec;

public class GameplaySpec {
    public final ModConfigSpec.EnumValue<Tiers> blockOperationsModuleToolTier;
    public final ModConfigSpec.LongValue soundCardCoolDownSeconds;
    public final ModConfigSpec.IntValue cpuFrequencyTier1;
    public final ModConfigSpec.IntValue cpuFrequencyTier2;
    public final ModConfigSpec.IntValue cpuFrequencyTier3;
    public final ModConfigSpec.IntValue cpuFrequencyTier4;
    public final ModConfigSpec.IntValue monitorMaxWidth;
    public final ModConfigSpec.IntValue monitorMaxHeight;
    public final ModConfigSpec.IntValue monitorFps;

    GameplaySpec(ModConfigSpec.Builder builder) {
        blockOperationsModuleToolTier =
                builder.comment("The mining tool equivalent of the block operations module")
                        .defineEnum("blockOperationsModuleToolTier", Tiers.DIAMOND);

        soundCardCoolDownSeconds =
                builder.comment(
                                "The number of seconds between sound card uses, to prevent"
                                        + " spam/abuse")
                        .defineInRange("soundCardCoolDownSeconds", 2, 1, Long.MAX_VALUE);

        monitorMaxWidth =
                builder.comment(
                                "Maximum width (in blocks) a monitor multiblock can grow to when"
                                        + " placing monitors next to each other. The blockstate"
                                        + " property range is always 1..8, so monitors wider than"
                                        + " this configured limit can still be loaded from existing"
                                        + " saves.")
                        .defineInRange("monitorMaxWidth", 5, 1, 8);
        monitorMaxHeight =
                builder.comment(
                                "Maximum height (in blocks) a monitor multiblock can grow to when"
                                        + " placing monitors next to each other. The blockstate"
                                        + " property range is always 1..8, so monitors taller than"
                                        + " this configured limit can still be loaded from existing"
                                        + " saves.")
                        .defineInRange("monitorMaxHeight", 5, 1, 8);

        monitorFps =
                builder.comment(
                                "Maximum rate at which monitor and projector frames are sent"
                                        + " to clients, in frames per second.")
                        .defineInRange("monitorFps", 20, 1, 60);

        cpuFrequencyTier1 =
                builder.comment(
                                "Frequency of the Tier 1 CPU (in megahertz).",
                                "Tier layout:",
                                "CPU T1: cpuFrequencyTier1",
                                "CPU T2: cpuFrequencyTier2",
                                "CPU T3: cpuFrequencyTier3",
                                "CPU T4: cpuFrequencyTier4",
                                "CPU T_INF is not configurable (creative, 1000 MHz).",
                                "With the default values this is 50MHz, 100MHz, 200MHz,"
                                        + " 400MHz respectively.")
                        .defineInRange("cpuFrequencyTier1", 50, 1, Integer.MAX_VALUE);
        cpuFrequencyTier2 =
                builder.comment("Frequency of the Tier 2 CPU (in megahertz).")
                        .defineInRange("cpuFrequencyTier2", 100, 1, Integer.MAX_VALUE);
        cpuFrequencyTier3 =
                builder.comment("Frequency of the Tier 3 CPU (in megahertz).")
                        .defineInRange("cpuFrequencyTier3", 200, 1, Integer.MAX_VALUE);
        cpuFrequencyTier4 =
                builder.comment("Frequency of the Tier 4 CPU (in megahertz).")
                        .defineInRange("cpuFrequencyTier4", 400, 1, Integer.MAX_VALUE);
    }

    public void loadValues() {
        Config.blockOperationsModuleToolTier = blockOperationsModuleToolTier.get().name();
        Config.soundCardCoolDownSeconds = soundCardCoolDownSeconds.get();
        Config.cpuFrequencyTier1 = cpuFrequencyTier1.get() * 1_000_000;
        Config.cpuFrequencyTier2 = cpuFrequencyTier2.get() * 1_000_000;
        Config.cpuFrequencyTier3 = cpuFrequencyTier3.get() * 1_000_000;
        Config.cpuFrequencyTier4 = cpuFrequencyTier4.get() * 1_000_000;
        Config.monitorMaxWidth = monitorMaxWidth.get();
        Config.monitorMaxHeight = monitorMaxHeight.get();
        Config.monitorFps = monitorFps.get();
    }
}