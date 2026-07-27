package li.cil.oc2.common.config.common;

import li.cil.oc2.common.config.Config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VXLANSpec {
    public final ModConfigSpec.BooleanValue enable;
    public final ModConfigSpec.ConfigValue<String> remoteHost;
    public final ModConfigSpec.IntValue remotePort;
    public final ModConfigSpec.ConfigValue<String> bindHost;
    public final ModConfigSpec.IntValue bindPort;

    VXLANSpec(ModConfigSpec.Builder builder) {
        enable =
                builder.comment(
                                "Whether to enable VXLAN support, must be on for the internet card"
                                    + " to work")
                        .define("enable", false);

        remoteHost =
                builder.comment("The remote host that the VXLAN protocol is running on")
                        .define("remoteHost", "::1");

        remotePort =
                builder.comment("The remote port that the VXLAN protocol is exposed on")
                        .defineInRange("remotePort", 4789, 1, 65535);

        bindHost = builder.comment("The address to bind VXLAN to").define("bindHost", "::1");

        bindPort =
                builder.comment("The port to bind VXLAN to")
                        .defineInRange("bindPort", 4789, 1, 65535);
    }

    public void loadValues() {
        Config.enable = enable.get();
        Config.remoteHost = remoteHost.get();
        Config.remotePort = remotePort.get();
        Config.bindHost = bindHost.get();
        Config.bindPort = bindPort.get();
    }
}
