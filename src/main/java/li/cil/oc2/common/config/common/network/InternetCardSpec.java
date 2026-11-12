package li.cil.oc2.common.config.common.network;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import li.cil.oc2.common.config.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public class InternetCardSpec {
    private static final String DEFAULT_NAME_SERVER = String.format("%d.%d.%d.%d", 1, 1, 1, 1);

    public final ModConfigSpec.BooleanValue internetCardEnabled;
    public final ModConfigSpec.IntValue defaultSessionLifetimeMs;
    public final ModConfigSpec.IntValue tcpEstablishedLifetimeMs;
    public final ModConfigSpec.IntValue datagramSessionLifetimeMs;
    public final ModConfigSpec.IntValue datagramSessionsNumberPerCardLimit;
    public final ModConfigSpec.IntValue defaultSessionsNumberPerCardLimit;
    public final ModConfigSpec.IntValue defaultSessionsNumberLimit;
    public final ModConfigSpec.IntValue defaultEchoRequestTimeoutMs;
    public final ModConfigSpec.ConfigValue<List<? extends String>> deniedHosts;
    public final ModConfigSpec.ConfigValue<List<? extends String>> allowedHosts;
    public final ModConfigSpec.ConfigValue<String> defaultNameServer;
    public final ModConfigSpec.BooleanValue useSynchronisedNAT;
    public final ModConfigSpec.IntValue streamBufferSize;
    public final ModConfigSpec.IntValue tcpRetransmissionTimeoutMs;

    public InternetCardSpec(ModConfigSpec.Builder builder) {
        internetCardEnabled =
                builder.comment(
                                "Whether to enable the internet card and the internet gateway.",
                                "They reach the internet directly from the server and do not need"
                                        + " the VXLAN tunnel, which is a separate feature")
                        .define("internetCardEnabled", false);

        defaultSessionLifetimeMs =
                builder.comment(
                                "Lifetime in milliseconds of a session that is idle, for connections"
                                        + " that are being opened or closed")
                        .defineInRange("defaultSessionLifetimeMs", 60 * 1000, 0, Integer.MAX_VALUE);

        tcpEstablishedLifetimeMs =
                builder.comment(
                                "Idle lifetime in milliseconds of an established TCP connection.",
                                "Much longer than the default because a quiet SSH session or a"
                                        + " long poll must not be dropped; NAT gateways use one to"
                                        + " two hours")
                        .defineInRange(
                                "tcpEstablishedLifetimeMs", 60 * 60 * 1000, 0, Integer.MAX_VALUE);

        datagramSessionLifetimeMs =
                builder.comment(
                                "Idle lifetime in milliseconds of a UDP exchange such as a DNS"
                                        + " lookup, which gets a fresh source port every time")
                        .defineInRange("datagramSessionLifetimeMs", 15 * 1000, 0, Integer.MAX_VALUE);

        datagramSessionsNumberPerCardLimit =
                builder.comment(
                                "Number of UDP exchanges tracked per internet card.",
                                "They are counted apart from connections; when the limit is reached"
                                        + " the oldest exchange is dropped instead of refusing the new"
                                        + " one")
                        .defineInRange(
                                "datagramSessionsNumberPerCardLimit", 64, 1, Integer.MAX_VALUE);

        defaultSessionsNumberPerCardLimit =
                builder.comment(
                                "Number of connections allowed per internet card; UDP exchanges"
                                        + " are limited separately")
                        .defineInRange(
                                "defaultSessionsNumberPerCardLimit", 32, 0, Integer.MAX_VALUE);

        defaultSessionsNumberLimit =
                builder.comment(
                                "Number of sessions (connections) allowed in total across all"
                                        + " cards")
                        .defineInRange("defaultSessionsNumberLimit", 100, 0, Integer.MAX_VALUE);

        defaultEchoRequestTimeoutMs =
                builder.comment(
                                "Number of milliseconds before a timeout should be assumed on"
                                        + " ICMP/Echo (ping) packets")
                        .defineInRange("defaultEchoRequestTimeoutMs", 1000, 1, Integer.MAX_VALUE);

        deniedHosts =
                builder.comment(
                                "A list of hosts (IPs) that VMs are not allowed to access",
                                "By default all local, link-local and reserved address ranges are"
                                        + " disallowed; we recommend leaving it this way",
                                "169.254.0.0/16 also blocks cloud-metadata services (e.g."
                                        + " 169.254.169.254) reachable from publicly hosted servers",
                                "The limited broadcast address 255.255.255.255 is deliberately NOT"
                                        + " denied: DHCP relies on it",
                                "Only denied hosts or allowed hosts may have a value, or an error"
                                        + " will occur",
                                "Note: hostnames are resolved once when the config loads and are"
                                        + " therefore vulnerable to DNS rebinding; CIDR ranges are"
                                        + " recommended over names")
                        .defineList(
                                "deniedHosts",
                                Arrays.asList(
                                        "0.0.0.0/8",
                                        "127.0.0.0/8",
                                        "10.0.0.0/8",
                                        "100.64.0.0/10",
                                        "169.254.0.0/16",
                                        "172.16.0.0/12",
                                        "192.168.0.0/16",
                                        "224.0.0.0/4",
                                        "192.0.2.0/24",
                                        "198.51.100.0/24",
                                        "203.0.113.0/24"),
                                String::new,
                                obj -> obj instanceof String && !((String) obj).isBlank());

        allowedHosts =
                builder.comment(
                                "A list of hosts (IPs) that VMs are allowed to access",
                                "Only denied hosts or allowed hosts may have a value, or an error"
                                        + " will occur")
                        .defineList(
                                "allowedHosts",
                                List.of(),
                                String::new,
                                obj -> obj instanceof String && !((String) obj).isBlank());

        defaultNameServer =
                builder.comment("The default nameserver to be used")
                        .define("defaultNameServer", DEFAULT_NAME_SERVER);

        useSynchronisedNAT = builder.define("useSynchronisedNAT", false);

        streamBufferSize =
                builder.comment(
                                "Size of the TCP stream send/receive buffers in bytes",
                                "Larger buffers allow higher throughput at the cost of memory")
                        .defineInRange("streamBufferSize", 32 * 1024, 1, Integer.MAX_VALUE);

        tcpRetransmissionTimeoutMs =
                builder.defineInRange("tcpRetransmissionTimeoutMs", 2000, 1, Integer.MAX_VALUE);
    }

    @SuppressFBWarnings(
            value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
            justification = "The spec copies the loaded values into the process-wide configuration.")
    public void loadValues() {
        Config.internetCardEnabled = internetCardEnabled.get();
        Config.defaultSessionLifetimeMs = defaultSessionLifetimeMs.get();
        Config.tcpEstablishedLifetimeMs = tcpEstablishedLifetimeMs.get();
        Config.datagramSessionLifetimeMs = datagramSessionLifetimeMs.get();
        Config.datagramSessionsNumberPerCardLimit = datagramSessionsNumberPerCardLimit.get();
        Config.defaultSessionsNumberPerCardLimit = defaultSessionsNumberPerCardLimit.get();
        Config.defaultSessionsNumberLimit = defaultSessionsNumberLimit.get();
        Config.defaultEchoRequestTimeoutMs = defaultEchoRequestTimeoutMs.get();
        Config.deniedHosts =
                deniedHosts.get().stream().map(String::valueOf).collect(Collectors.toList());
        Config.allowedHosts =
                allowedHosts.get().stream().map(String::valueOf).collect(Collectors.toList());
        Config.defaultNameServer = defaultNameServer.get();
        Config.useSynchronisedNAT = useSynchronisedNAT.get();
        Config.streamBufferSize = streamBufferSize.get();
        Config.tcpRetransmissionTimeoutMs = tcpRetransmissionTimeoutMs.get();
    }
}