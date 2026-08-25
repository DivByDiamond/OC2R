package li.cil.oc2.common.blockentity.network.switches.port;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SwitchLog {
    private static final Logger LOGGER = LogManager.getLogger();

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
            LOGGER.debug(
                    "Switch Packet {} (Port {}, VLAN {}) -> {} drop ({})",
                    inMac, ingressSide, ingressVlan, outMac, reason);
        } else {
            LOGGER.debug(
                    "Switch Packet {} (Port {}, VLAN {}) -> {} (Port {}) drop ({})",
                    inMac, ingressSide, ingressVlan, outMac, egressSide, reason);
        }
    }

    public void emit() {
        if (!ENABLED) return;
        String inMac = PacketProcessor.macLongToString(srcMac);
        String outMac = PacketProcessor.macLongToString(destMac);
        LOGGER.debug(
                "Switch Packet {} (Port {}, VLAN {}) -> {} (Port {}, VLAN {})",
                inMac, ingressSide, ingressVlan, outMac, egressSide, egressVlan);
    }

    public void flood() {
        if (!ENABLED) return;
        String inMac = PacketProcessor.macLongToString(srcMac);
        String outMac = PacketProcessor.macLongToString(destMac);
        LOGGER.debug(
                "Switch Packet {} (Port {}, VLAN {}) -> {} flood",
                inMac, ingressSide, ingressVlan, outMac);
    }
}
