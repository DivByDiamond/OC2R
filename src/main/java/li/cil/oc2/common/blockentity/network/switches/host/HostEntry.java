package li.cil.oc2.common.blockentity.network.switches.host;

public final class HostEntry {
    public final int iface;
    public final long timestamp;

    public HostEntry(int iface, long timestamp) {
        this.iface = iface;
        this.timestamp = timestamp;
    }
}
