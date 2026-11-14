package li.cil.oc2.common.config;

/** Stand-in for the mod's Config: TunnelManager reads only these fields, and the real class needs NeoForge. */
public final class Config {
    public static boolean enable;
    public static String bindHost = "0.0.0.0";
    public static int bindPort = 4789;
    public static String remoteHost = "127.0.0.1";
    public static int remotePort = 4789;
    public static int vxlanPacketQueueCapacity = 32;

    private Config() {
    }
}
