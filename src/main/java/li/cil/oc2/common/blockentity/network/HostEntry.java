package li.cil.oc2.common.blockentity.network;

final class HostEntry {
    public final int iface;
    public final long timestamp;

    HostEntry(int iface, long timestamp) {
        this.iface = iface;
        this.timestamp = timestamp;
    }
}
