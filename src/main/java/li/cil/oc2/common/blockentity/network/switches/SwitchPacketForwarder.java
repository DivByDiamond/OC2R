package li.cil.oc2.common.blockentity.network.switches;

import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.network.switches.host.HostEntry;
import li.cil.oc2.common.blockentity.network.switches.port.PacketProcessor;
import li.cil.oc2.common.blockentity.network.switches.port.PortSettings;
import li.cil.oc2.common.blockentity.network.switches.port.SwitchLog;

final class SwitchPacketForwarder {
    private final NetworkSwitchBlockEntity switchEntity;

    SwitchPacketForwarder(final NetworkSwitchBlockEntity switchEntity) {
        this.switchEntity = switchEntity;
    }

    void forward(final NetworkInterface source, byte[] frame_bytes, final int timeToLive) {
        switchEntity.validateAdjacentBlocks();
        long tickTime = switchEntity.getLevel().getGameTime();
        long destMac = PacketProcessor.macToLong(frame_bytes, 0);
        long srcMac = PacketProcessor.macToLong(frame_bytes, 6);
        short vlan = PacketProcessor.getVLAN(frame_bytes);
        Optional<Integer> optSide = switchEntity.sideReverseLookup(source);
        if (!optSide.isPresent()) return;
        int side = optSide.get();
        switchEntity.hostTable.put(srcMac, side, tickTime);
        PortSettings ingressSettings = switchEntity.portManager.portSettings[side];
        SwitchLog log = new SwitchLog(vlan, side, srcMac, destMac);
        byte[] frame = frame_bytes;
        if (vlan == 0) {
            Pair<Short, byte[]> pair = PacketProcessor.removeVLANTag(frame);
            frame = pair.getSecond();
            if (ingressSettings.untagged != 0) {
                frame = PacketProcessor.addVLANTag(frame, ingressSettings.untagged);
                vlan = ingressSettings.untagged;
            }
        } else {
            if (!(ingressSettings.trunkAll || ingressSettings.tagged.contains(vlan))) {
                log.drop("Tag not allowed for ingress");
                return;
            }
        }
        HostEntry host = switchEntity.hostTable.get(destMac);
        if (host != null) {
            if (host.iface == side && !ingressSettings.hairpin) {
                log.drop("hairpin disabled");
                return;
            }
            writeToSide(frame, host.iface, vlan, log, timeToLive);
        } else {
            log.flood();
            for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++)
                if (i != side) writeToSide(frame, i, vlan, log, timeToLive);
        }
    }

    private void writeToSide(byte[] frame, int side, short vlan, SwitchLog log, int timeToLive) {
        log.egressPort(side);
        NetworkInterface iface = switchEntity.adjacentBlockInterfaces[side];
        if (iface != null) {
            PortSettings egressSettings = switchEntity.portManager.portSettings[side];
            if (egressSettings.untagged != 0 && vlan == 0) {
                log.drop("inner tag untagged");
                return;
            }
            byte[] egressFrame;
            if (egressSettings.untagged == vlan) {
                Pair<Short, byte[]> pair = PacketProcessor.removeVLANTag(frame);
                egressFrame = pair.getSecond();
                log.egressVlan = 0;
            } else if (!(egressSettings.trunkAll || egressSettings.tagged.contains(vlan))) {
                log.drop("Tag not allowed for egress");
                return;
            } else {
                egressFrame = frame;
                log.egressVlan = vlan;
            }
            log.emit();
            iface.writeEthernetFrame(switchEntity, egressFrame, timeToLive - switchEntity.TTL_COST);
        }
    }
}
