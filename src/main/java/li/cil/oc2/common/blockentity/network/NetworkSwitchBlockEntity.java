package li.cil.oc2.common.blockentity.network;

import static java.util.Collections.singletonList;

import com.mojang.datafixers.util.Pair;
import java.util.*;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

public final class NetworkSwitchBlockEntity extends ModBlockEntity
        implements NamedDevice, DocumentedDevice, NetworkInterface, TickableBlockEntity {
    private final long HOST_TTL = 20 * 60 * 2;
    private final int TTL_COST = 1;
    private final SwitchHostTable hostTable = new SwitchHostTable();
    private final SwitchPortManager portManager = new SwitchPortManager();
    private int tickCount = 0;
    private final NetworkInterface[] adjacentBlockInterfaces =
            new NetworkInterface[Constants.BLOCK_FACE_COUNT];
    private BlockCapabilityCache<NetworkInterface, Direction>[] adjacentBlockCaches = null;
    private boolean haveAdjacentBlocksChanged = true;

    public NetworkSwitchBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.NETWORK_SWITCH.get(), pos, state);
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        final BlockPos pos = getBlockPos();
        var adj =
                new ArrayList<BlockCapabilityCache<NetworkInterface, Direction>>(
                        Constants.BLOCK_FACE_COUNT);
        for (var side : Constants.DIRECTIONS)
            adj.set(
                    side.get3DDataValue(),
                    BlockCapabilityCache.create(
                            Capabilities.NetworkInterface.BLOCK,
                            (ServerLevel) level,
                            pos.relative(side),
                            side.getOpposite(),
                            () -> !this.isRemoved(),
                            this::handleNeighborChanged));
        adjacentBlockCaches = adj.toArray(new BlockCapabilityCache[0]);
    }

    @Override
    public void writeEthernetFrame(
            final NetworkInterface source, byte[] frame_bytes, final int timeToLive) {
        validateAdjacentBlocks();
        long tickTime = getLevel().getGameTime();
        long destMac = PacketProcessor.macToLong(frame_bytes, 0);
        long srcMac = PacketProcessor.macToLong(frame_bytes, 6);
        short vlan = PacketProcessor.getVLAN(frame_bytes);
        Optional<Integer> optSide = sideReverseLookup(source);
        if (!optSide.isPresent()) return;
        int side = optSide.get();
        hostTable.put(srcMac, side, tickTime);
        PortSettings ingressSettings = portManager.portSettings[side];
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
        HostEntry host = hostTable.get(destMac);
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

    @Override
    public byte[] readEthernetFrame() {
        return new byte[0];
    }

    private void writeToSide(byte[] frame, int side, short vlan, SwitchLog log, int timeToLive) {
        log.egressPort(side);
        NetworkInterface iface = adjacentBlockInterfaces[side];
        if (iface != null) {
            PortSettings egressSettings = portManager.portSettings[side];
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
            iface.writeEthernetFrame(this, egressFrame, timeToLive - TTL_COST);
        }
    }

    @Override
    public void clientTick() {}

    @Override
    public void serverTick() {
        if (level == null) return;
        tickCount++;
        if ((tickCount) % 20 == 1) {
            long threshold = getLevel().getGameTime() - HOST_TTL;
            if (threshold < 0) return;
            hostTable.removeExpired(threshold);
        }
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        visitor.visitCallback("getHostTable")
                .description("Returns the MAC address table of the switch")
                .returnValueDescription(
                        "The MAC table. For each host the mac address, the age (in ticks) and the"
                                + " face is returned");
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return singletonList("switch");
    }

    @Override
    public void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        List<Tag> hosts = new ListTag();
        hostTable.save(hosts);
        tag.put("hosts", (ListTag) hosts);
        List<Tag> ports = new ListTag();
        portManager.save(ports);
        tag.put("ports", (ListTag) ports);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Tag hosts = tag.get("hosts");
        if (hosts != null) hostTable.load((List<Tag>) hosts);
        Tag ports = tag.get("ports");
        if (ports != null) portManager.load((List<Tag>) ports);
    }

    @Callback(name = "getHostTable")
    public List<LuaHostEntry> getHostTable() {
        return hostTable.getHostTable(getLevel().getGameTime());
    }

    @Callback(name = "getPortConfig", synchronize = false)
    public PortSettings[] getPortSettings() {
        return portManager.getPortSettings();
    }

    @Callback(name = "setPortConfig")
    public void setPortSettings(List<Map> settings) {
        portManager.setPortSettings(settings);
    }

    @Callback(name = "getLinkState")
    public boolean[] getLinkState() {
        validateAdjacentBlocks();
        boolean[] sides = new boolean[Constants.BLOCK_FACE_COUNT];
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++)
            sides[i] = adjacentBlockInterfaces[i] != null;
        return sides;
    }

    private Optional<Integer> sideReverseLookup(NetworkInterface iface) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++)
            if (iface.equals(adjacentBlockInterfaces[i])) return Optional.of(i);
        return Optional.empty();
    }

    private void validateAdjacentBlocks() {
        if (isRemoved() || !haveAdjacentBlocksChanged) return;
        for (final Direction side : Constants.DIRECTIONS)
            adjacentBlockInterfaces[side.get3DDataValue()] = null;
        haveAdjacentBlocksChanged = false;
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < adjacentBlockCaches.length; i++)
            adjacentBlockInterfaces[i] = adjacentBlockCaches[i].getCapability();
    }

    private void handleNeighborChanged() {
        haveAdjacentBlocksChanged = true;
    }
}