package li.cil.oc2.common.config.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import li.cil.oc2.common.config.Config;
//? if >=26.1 {
/*import li.cil.oc2.common.config.Tiers;
*///?}
import li.cil.oc2.common.vm.video.VideoCodec;
//? if <26.1 {
import net.minecraft.world.item.Tiers;
//?}
import net.neoforged.neoforge.common.ModConfigSpec;

public class GameplaySpec {
    public final ModConfigSpec.BooleanValue relativeCoordinates;
    public final ModConfigSpec.IntValue gpsTowerRange;
    public final ModConfigSpec.IntValue robotDockRange;
    public final ModConfigSpec.IntValue radioInviteLifetimeTicks;
    public final ModConfigSpec.IntValue radioHopDelayTicks;
    public final ModConfigSpec.IntValue radioMaxHops;
    public final ModConfigSpec.IntValue radioMaxFrameBytes;
    public final ModConfigSpec.IntValue radioLinkBytesPerTick;
    public final ModConfigSpec.IntValue radioMaxDeliveryTicks;
    public final ModConfigSpec.IntValue radioQueueLimit;
    public final ModConfigSpec.IntValue radioNetworkQueueLimit;
    public final ModConfigSpec.IntValue radioDeviceQueueLimit;
    public final ModConfigSpec.IntValue radioPacketLifetimeTicks;

    public final ModConfigSpec.EnumValue<Tiers> blockOperationsModuleToolTier;
    public final ModConfigSpec.LongValue soundCardCoolDownSeconds;
    public final ModConfigSpec.IntValue soundCardPcmBytesPerSecond;
    public final ModConfigSpec.IntValue cpuFrequencyTier1;
    public final ModConfigSpec.IntValue cpuFrequencyTier2;
    public final ModConfigSpec.IntValue cpuFrequencyTier3;
    public final ModConfigSpec.IntValue cpuFrequencyTier4;
    public final ModConfigSpec.IntValue maxBusElements;
    public final ModConfigSpec.IntValue networkConnectorPorts;
    public final ModConfigSpec.IntValue monitorMaxWidth;
    public final ModConfigSpec.IntValue monitorMaxHeight;
    public final ModConfigSpec.IntValue monitorFps;
    public final ModConfigSpec.ConfigValue<String> videoCodec;

    GameplaySpec(ModConfigSpec.Builder builder) {
        relativeCoordinates =
                builder.comment(
                                "GPS towers report offsets from their explicitly selected network reference.",
                                "Relative mode hides absolute world XYZ from this API.",
                                "Robot positions already use their own placement/calibration origin.",
                                "This controls the device API, not Minecraft's world save format.")
                        .define("relative_coordinates", false);

        gpsTowerRange =
                builder.comment("Maximum horizontal distance in blocks between paired GPS towers.",
                                "Circular X/Z coverage; height is ignored.",
                                "Unloaded stations relay using their persisted membership without loading chunks.")
                        .defineInRange("gps_tower_range", 45, 1, 1024);

        robotDockRange =
                builder.comment("Horizontal client attachment radius in blocks of a robot dock station (the charger).",
                                "Circular X/Z coverage; height is ignored.",
                                "A dock attaches robots in range and reaches the network "
                                        + "through the nearest tower in tower range.",
                                "It does not relay between towers and the radii are not added together.",
                                "Only loaded docks count; the bound keeps the search to nearby loaded chunks.")
                        .defineInRange("robot_dock_range", 10, 1, 64);

        radioInviteLifetimeTicks = builder.comment("Radio invitation lifetime in server ticks.")
                .defineInRange("radioInviteLifetimeTicks", 1200, 1, 72000);
        radioHopDelayTicks = builder.comment("Base server ticks per radio hop.",
                "One tick is nominally 50 ms at 20 TPS.")
                .defineInRange("radioHopDelayTicks", 1, 1, 200);
        radioMaxHops = builder.comment("Maximum radio hop TTL.")
                .defineInRange("radioMaxHops", 64, 1, 256);
        radioMaxFrameBytes = builder.comment("Maximum Ethernet frame bytes accepted by the radio bridge.")
                .defineInRange("radioMaxFrameBytes", 2048, 64, 65536);
        radioLinkBytesPerTick = builder.comment("Radio link bandwidth per sender in bytes per server tick "
                        + "(1024 is about 20 KB/s at 20 TPS).")
                .defineInRange("radioLinkBytesPerTick", 1024, 1, 1048576);
        radioMaxDeliveryTicks = builder.comment("Upper bound on the propagation delay in server ticks, "
                        + "regardless of hop count (2 ticks = 0.1 s).")
                .defineInRange("radioMaxDeliveryTicks", 2, 1, 200);
        radioQueueLimit = builder.comment("Maximum queued deliveries per dimension.")
                .defineInRange("radioQueueLimit", 4096, 1, 65536);
        radioNetworkQueueLimit = builder.comment("Maximum queued deliveries per radio network.")
                .defineInRange("radioNetworkQueueLimit", 1024, 1, 65536);
        radioDeviceQueueLimit = builder.comment("Maximum queued deliveries involving one radio endpoint.")
                .defineInRange("radioDeviceQueueLimit", 256, 1, 65536);
        radioPacketLifetimeTicks = builder.comment("Maximum queued radio packet lifetime in server ticks.",
                "Restart after changing radio limits.")
                .defineInRange("radioPacketLifetimeTicks", 200, 1, 12000);

        blockOperationsModuleToolTier =
                builder.comment("The mining tool equivalent of the block operations module")
                        .defineEnum("blockOperationsModuleToolTier", Tiers.DIAMOND);

        soundCardCoolDownSeconds =
                builder.comment(
                                "The number of seconds between sound card uses, to prevent"
                                        + " spam/abuse")
                        .defineInRange("soundCardCoolDownSeconds", 2, 1, Long.MAX_VALUE);

        soundCardPcmBytesPerSecond =
                builder.comment(
                                "Maximum number of PCM bytes per second a single sound card"
                                        + " may stream to clients. Exceeding the budget throws"
                                        + " an exception back to the guest.")
                        .defineInRange(
                                "soundCardPcmBytesPerSecond",
                                128 * 1024,
                                4096,
                                16 * 1024 * 1024);

        maxBusElements =
                builder.comment(
                                "Maximum number of bus elements (cables, interfaces, devices) that"
                                        + " a single computer can reach over one connected bus."
                                        + " Elements beyond this limit are left out of the bus while"
                                        + " everything else keeps working.")
                        .defineInRange("maxBusElements", 1024, 16, 65536);

        networkConnectorPorts =
                builder.comment(
                                "Number of cables a single network connector can hold. Existing"
                                        + " connections are kept if this is lowered.")
                        .defineInRange("networkConnectorPorts", 4, 2, 8);

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

        videoCodec =
                builder.comment(
                                "Codec used to transmit monitor/projector frames.",
                                "delta - lossless tile-based delta compression of RGB565"
                                        + " (low CPU, low bandwidth; recommended).",
                                "raw - full RGB565 framebuffer per frame (lowest CPU, highest",
                                "       bandwidth, best for local/LAN play).",
                                "h264 - legacy H.264 via bundled jcodec (highest CPU).")
                        .define("videoCodec", "delta");

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

    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification = "Config is the static snapshot that every config reload republishes.")
    public void loadValues() {
        Config.relativeCoordinates = relativeCoordinates.get();
        Config.gpsTowerRange = gpsTowerRange.get();
        Config.robotDockRange = robotDockRange.get();
        Config.radioInviteLifetimeTicks = radioInviteLifetimeTicks.get();
        Config.radioHopDelayTicks = radioHopDelayTicks.get();
        Config.radioMaxHops = radioMaxHops.get();
        Config.radioMaxFrameBytes = radioMaxFrameBytes.get();
        Config.radioLinkBytesPerTick = radioLinkBytesPerTick.get();
        Config.radioMaxDeliveryTicks = radioMaxDeliveryTicks.get();
        Config.radioQueueLimit = radioQueueLimit.get();
        Config.radioNetworkQueueLimit = radioNetworkQueueLimit.get();
        Config.radioDeviceQueueLimit = radioDeviceQueueLimit.get();
        Config.radioPacketLifetimeTicks = radioPacketLifetimeTicks.get();

        Config.blockOperationsModuleToolTier = blockOperationsModuleToolTier.get().name();
        Config.soundCardCoolDownSeconds = soundCardCoolDownSeconds.get();
        Config.soundCardPcmBytesPerSecond = soundCardPcmBytesPerSecond.get();
        Config.cpuFrequencyTier1 = cpuFrequencyTier1.get() * 1_000_000;
        Config.cpuFrequencyTier2 = cpuFrequencyTier2.get() * 1_000_000;
        Config.cpuFrequencyTier3 = cpuFrequencyTier3.get() * 1_000_000;
        Config.cpuFrequencyTier4 = cpuFrequencyTier4.get() * 1_000_000;
        Config.maxBusElements = maxBusElements.get();
        Config.networkConnectorPorts = networkConnectorPorts.get();
        Config.monitorMaxWidth = monitorMaxWidth.get();
        Config.monitorMaxHeight = monitorMaxHeight.get();
        Config.monitorFps = monitorFps.get();
        Config.videoCodec = parseVideoCodec(videoCodec.get());
    }

    private static int parseVideoCodec(final String value) {
        try {
            return VideoCodec.valueOf(value.toUpperCase(Locale.ROOT)).id;
        } catch (final IllegalArgumentException e) {
            return VideoCodec.RAW.id;
        }
    }
}
