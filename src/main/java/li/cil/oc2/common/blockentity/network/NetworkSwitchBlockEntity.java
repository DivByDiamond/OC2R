package li.cil.oc2.common.blockentity.network;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.internal.LinkedTreeMap;
import com.mojang.datafixers.util.Pair;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;


import static java.util.Collections.singletonList;

public final class NetworkSwitchBlockEntity extends ModBlockEntity implements NamedDevice, DocumentedDevice, NetworkInterface, TickableBlockEntity {
    private final String GET_LINK_STATE = "getLinkState";
    private final String GET_HOST_TABLE = "getHostTable";
    private final String GET_PORT_CONFIG = "getPortConfig";
    private final String SET_PORT_CONFIG = "setPortConfig";

    private final long HOST_TTL = 20 * 60 * 2;
    private final int TTL_COST = 1;
    private final Map<Long, HostEntry> hostTable = new HashMap<>();
    private final PortSettings[] portSettings = new PortSettings[Constants.BLOCK_FACE_COUNT];
    private int tickCount = 0;
    private final NetworkInterface[] adjacentBlockInterfaces = new NetworkInterface[Constants.BLOCK_FACE_COUNT];
    private BlockCapabilityCache<NetworkInterface, Direction>[] adjacentBlockCaches = null;
    private boolean haveAdjacentBlocksChanged = true;

    public NetworkSwitchBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.NETWORK_SWITCH.get(), pos, state);
        for (int i = 0; i < portSettings.length; i++) {
            portSettings[i] = new PortSettings();
        }
    }

    @Override
    protected void loadServer() {
        super.loadServer();

        final BlockPos pos = getBlockPos();
        var adj = new ArrayList<BlockCapabilityCache<NetworkInterface, Direction>>();
        for (var side : Constants.DIRECTIONS) {
            adj.add(null);
        }
        for (var side : Constants.DIRECTIONS) {
            adj.set(side.get3DDataValue(), BlockCapabilityCache.create(
                Capabilities.NetworkInterface.BLOCK,
                (ServerLevel) level,
                pos.relative(side),
                side.getOpposite(),
                () -> !this.isRemoved(),
                this::handleNeighborChanged
            ));
        }

        adjacentBlockCaches = (BlockCapabilityCache<NetworkInterface, Direction>[]) adj.toArray();
    }

    public void writeEthernetFrame(final NetworkInterface source, byte[] frame, final int timeToLive) {
        validateAdjacentBlocks();
        long tickTime = getLevel().getGameTime();
        long destMac = macToLong(frame, 0);
        long srcMac = macToLong(frame, 6);
        short vlan = getVLAN(frame);
        Optional<Integer> optSide = sideReverseLookup(source);
        if (!optSide.isPresent()) {
            return;
        }
        int side = optSide.get();
        if (hostTable.size() <= 256) {
            hostTable.put(srcMac, new HostEntry(side, tickTime));
        }
        PortSettings ingressSettings = portSettings[side];
        SwitchLog log = new SwitchLog(vlan, side, srcMac, destMac);
        if (vlan == 0) {
            // Untagged packet
            Pair<Short, byte[]> pair = removeVLANTag(frame); // Remove tag in case there is a 0-tag
            frame = pair.getSecond();
            if (ingressSettings.untagged != 0) {
                frame = addVLANTag(frame, ingressSettings.untagged);
                vlan = ingressSettings.untagged;
            }
        } else {
            if (!(ingressSettings.trunkAll || ingressSettings.tagged.contains(vlan))) {
                // drop packet with disallowed vlan
                log.drop("Tag not allowed for ingress");
                return;
            }
        }

        HostEntry host = hostTable.get(destMac);
        if (host != null) {
            if (host.iface == side && !ingressSettings.hairpin) {
                // if packet is to same port and hairpin is disabled, drop
                log.drop("hairpin disabled");
                return;
            }
            writeToSide(frame, host.iface, vlan, log, timeToLive);
        } else {
            log.flood();
            for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
                if (i != side) {
                    writeToSide(frame, i, vlan, log, timeToLive);
                }
            }
        }
    }

    @Override
    public byte[] readEthernetFrame() {
        return null;
    }

    private void writeToSide(byte[] frame, int side, short vlan, SwitchLog log, int timeToLive) {
        log.egressPort(side);
        NetworkInterface iface = adjacentBlockInterfaces[side];
        if (iface != null) {
            PortSettings egressSettings = portSettings[side];
            if (egressSettings.untagged != 0 && vlan == 0) {
                log.drop("inner tag untagged");
                return;
            }

            if (egressSettings.untagged == vlan) {
                Pair<Short, byte[]> pair = removeVLANTag(frame);
                frame = pair.getSecond();
                log.egressVlan = 0;
            } else if (!(egressSettings.trunkAll || egressSettings.tagged.contains(vlan))) {
                // Drop packets with wrong tag
                log.drop("Tag not allowed for egress");
                return;
            } else {
                log.egressVlan = vlan;
            }
            log.emit();
            iface.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
        }
    }

    private long macToLong(final byte[] mac, int offset) {
        long ret = 0;
        for (int i = 0; i < 6; i++) {
            ret |= ((((long) mac[i + offset]) & 0xff) << (i * 8));
        }
        return ret;
    }

    @Override
    public void clientTick()
    {
        return;
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }
        if (tickCount++ % 20 == 0) {
            long threshold = getLevel().getGameTime() - HOST_TTL;
            if (threshold < 0) {
                return;
            }
            hostTable.entrySet().removeIf(e -> e.getValue().timestamp < threshold);
        }
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        visitor.visitCallback(GET_HOST_TABLE)
            .description("Returns the MAC address table of the switch")
            .returnValueDescription("The MAC table. For each host the mac address, the age (in ticks) and the face is returned");
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return singletonList("switch");
    }

    @Override
    public void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        ListTag hosts = new ListTag();
        for (Map.Entry<Long, HostEntry> host : hostTable.entrySet()) {
            CompoundTag thisHost = new CompoundTag();
            thisHost.put("mac", LongTag.valueOf(host.getKey()));
            thisHost.put("side", IntTag.valueOf(host.getValue().iface));
            thisHost.put("timestamp", LongTag.valueOf(host.getValue().timestamp));
            hosts.add(thisHost);
        }
        tag.put("hosts", hosts);

        ListTag ports = new ListTag();
        for (PortSettings myPort : portSettings) {
            CompoundTag port = new CompoundTag();
            myPort.save(port);
            ports.add(port);
        }
        tag.put("ports", ports);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        Tag hosts = tag.get("hosts");
        if (hosts != null) {
            for (Tag host_ : ((ListTag) hosts)) {
                CompoundTag host = (CompoundTag) host_;
                hostTable.put(
                    host.getLong("mac"),
                    new HostEntry(
                        tag.getInt("side"),
                        tag.getLong("timestamp")
                    )
                );
            }
        }

        Tag ports = tag.get("ports");
        if (ports != null) {
            int i = 0;
            for (Tag port : ((ListTag) ports)) {
                portSettings[i++] = PortSettings.load((CompoundTag) port);
            }
        }

    }

    @Callback(name = GET_HOST_TABLE)
    public List<LuaHostEntry> getHostTable() {
        long now = getLevel().getGameTime();
        return hostTable
            .entrySet()
            .stream()
            .map(e -> new LuaHostEntry(macLongToString(e.getKey()), now - e.getValue().timestamp, e.getValue().iface))
            .collect(Collectors.toList());
    }

    @Callback(name = GET_PORT_CONFIG, synchronize = false)
    public PortSettings[] getPortSettings() {
        return portSettings;
    }

    @Callback(name = SET_PORT_CONFIG)
    public void setPortSettings(List<LinkedTreeMap> settings) {
        int max = Math.min(portSettings.length, settings.size());
        for (int i = 0; i < max; i++) {
            portSettings[i].untagged = ((Double) settings.get(i).get("untagged")).shortValue();
        }
    }

    @Callback(name = GET_LINK_STATE)
    public boolean[] getLinkState() {
        validateAdjacentBlocks();
        boolean[] sides = new boolean[Constants.BLOCK_FACE_COUNT];
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            sides[i] = adjacentBlockInterfaces[i] != null;
        }
        return sides;
    }

    private Optional<Integer> sideReverseLookup(NetworkInterface iface) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            if (iface == adjacentBlockInterfaces[i]) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    static String macLongToString(long mac) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i != 0) {
                ret.append(":");
            }
            ret.append(String.format("%02x", (mac >> (i * 8)) & 0xff));
        }
        return ret.toString();
    }

    private byte[] addVLANTag(byte[] packet, short tag) {
        if (tag != 0) {
            byte[] ret = new byte[packet.length + 4];
            copyBytes(packet, ret, 0, 0, 12); // Copy Ethernet Header
            copyBytes(packet, ret, 12, 16, packet.length - 12);
            ret[12] = (byte) 0x81;
            ret[13] = (byte) 0x00;
            ret[14] = (byte) ((tag >> 8) & 0x0f);
            ret[15] = (byte) (tag & 0xff);
            return ret;
        } else {
            return packet;
        }
    }

    private short getVLAN(byte[] packet) {
        if (packet[12] == ((byte) 0x81) && packet[13] == 0x00) {
            return (short) (packet[15] | ((((short) packet[14]) & 0x0f) << 8));
        } else {
            return (short) 0;
        }
    }

    private Pair<Short, byte[]> removeVLANTag(byte[] packet) {
        if (packet[12] == ((byte) 0x81) && packet[13] == 0x00) {
            byte[] ret = new byte[packet.length - 4];
            copyBytes(packet, ret, 0, 0, 12); // Copy Ethernet Header
            copyBytes(packet, ret, 16, 12, packet.length - 16); // Copy payload
            short tag = (short) (packet[15] | ((((short) packet[14]) & 0x0f) << 8)); // Extract vlan tag
            return new Pair<>(tag, ret);
        } else {
            return new Pair<>((short) 0, packet);
        }
    }

    private void copyBytes(byte[] input, byte[] output, int inputOffset, int outputOffset, int length) {
        if (length >= 0) System.arraycopy(input, inputOffset, output, outputOffset, length);
    }

    private void validateAdjacentBlocks() {
        if (isRemoved() || !haveAdjacentBlocksChanged) {
            return;
        }

        for (final Direction side : Constants.DIRECTIONS) {
            adjacentBlockInterfaces[side.get3DDataValue()] = null;
        }

        haveAdjacentBlocksChanged = false;

        if (level == null || level.isClientSide()) {
            return;
        }

        for (int i = 0; i < adjacentBlockCaches.length; i++) {
            adjacentBlockInterfaces[i] = adjacentBlockCaches[i].getCapability();
        }
    }

    private void handleNeighborChanged() {
        haveAdjacentBlocksChanged = true;
    }


}
