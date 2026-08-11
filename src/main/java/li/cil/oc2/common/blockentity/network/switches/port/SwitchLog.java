package li.cil.oc2.common.blockentity.network.switches.port;

public final class SwitchLog {
    private static final boolean ENABLED = true;
    short ingressVlan;
    public short egressVlan;
    int ingressSide;
    private final long srcMac;
    private final long destMac;
    public Integer egressSide = null;

    public SwitchLog(short ingressVlan, int ingressSide, long srcMac, long destMac) {
        this.ingressVlan = ingressVlan;
        this.ingressSide = ingressSide;
        this.srcMac = srcMac;
        this.destMac = destMac;
    }

    public void egressPort(int side) {
        egressSide = side;
    }

    public void drop(String reason) {
        if (!ENABLED) return;
        String inMac = PacketProcessor.macLongToString(srcMac);
        String outMac = PacketProcessor.macLongToString(destMac);
        if (egressSide == null) {
            System.out.printf(
                    "Switch Packet %s (Port %s, VLAN %s) -> %s drop (%s)\n",
                    inMac, ingressSide, ingressVlan, outMac, reason);
        } else {
            System.out.printf(
                    "Switch Packet %s (Port %s, VLAN %s) -> %s (Port %s) drop (%s)\n",
                    inMac, ingressSide, ingressVlan, outMac, egressSide, reason);
        }
    }

    public void emit() {
        if (!ENABLED) return;
        String inMac = PacketProcessor.macLongToString(srcMac);
        String outMac = PacketProcessor.macLongToString(destMac);
        System.out.printf(
                "Switch Packet %s (Port %s, VLAN %s) -> %s (Port %s, VLAN %s)\n",
                inMac, ingressSide, ingressVlan, outMac, egressSide, egressVlan);
    }

    public void flood() {
        if (!ENABLED) return;
        String inMac = PacketProcessor.macLongToString(srcMac);
        String outMac = PacketProcessor.macLongToString(destMac);
        System.out.printf(
                "Switch Packet %s (Port %s, VLAN %s) -> %s flood\n",
                inMac, ingressSide, ingressVlan, outMac);
    }
}
