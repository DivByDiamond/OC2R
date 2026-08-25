package li.cil.oc2.common.config.common.network;

import li.cil.oc2.common.config.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public class VXLANSpec {
    private static final String DEFAULT_VXLAN_HOST = String.format("::%d", 1);

    public final ModConfigSpec.BooleanValue enable;
    public final ModConfigSpec.ConfigValue<String> remoteHost;
    public final ModConfigSpec.IntValue remotePort;
    public final ModConfigSpec.ConfigValue<String> bindHost;
    public final ModConfigSpec.IntValue bindPort;
    public final ModConfigSpec.IntValue packetQueueCapacity;

    public VXLANSpec(ModConfigSpec.Builder builder) {
        enable =
                builder.comment(
                                "Whether to enable VXLAN support, must be on for the internet card"
                                        + " to work")
                        .define("enable", false);

        remoteHost =
                builder.comment("The remote host that the VXLAN protocol is running on")
                        .define("remoteHost", DEFAULT_VXLAN_HOST);

        remotePort =
                builder.comment("The remote port that the VXLAN protocol is exposed on")
                        .defineInRange("remotePort", 4789, 1, 65535);

        bindHost = builder.comment("The address to bind VXLAN to").define("bindHost", DEFAULT_VXLAN_HOST);

        bindPort =
                builder.comment("The port to bind VXLAN to")
                        .defineInRange("bindPort", 4789, 1, 65535);

        packetQueueCapacity =
                builder.comment(
                                "How many inbound frames are buffered per VXLAN hub between game"
                                        + " ticks; frames arriving beyond this are dropped")
                        .defineInRange("packetQueueCapacity", 32, 8, 4096);
    }

    public void loadValues() {
        Config.enable = enable.get();
        Config.remoteHost = remoteHost.get();
        Config.remotePort = remotePort.get();
        Config.bindHost = bindHost.get();
        Config.bindPort = bindPort.get();
        Config.vxlanPacketQueueCapacity = packetQueueCapacity.get();
    }
}