package li.cil.oc2.common.blockentity.network.switches.host;

public class LuaHostEntry {
    public final String mac;
    public final long age;
    public final int side;

    public LuaHostEntry(String mac, long age, int iface) {
        this.mac = mac;
        this.age = age;
        this.side = iface;
    }
}
