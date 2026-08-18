package li.cil.oc2.common.blockentity.network.switches;

import static java.util.Collections.singletonList;

import java.util.*;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.network.switches.host.LuaHostEntry;
import li.cil.oc2.common.blockentity.network.switches.port.PortSettings;
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
    private static final long HOST_TTL = 20 * 60 * 2;
    static final int TTL_COST = 1;
    final SwitchHostTable hostTable = new SwitchHostTable();
    final SwitchPortManager portManager = new SwitchPortManager();
    private int tickCount = 0;
    final NetworkInterface[] adjacentBlockInterfaces =
            new NetworkInterface[Constants.BLOCK_FACE_COUNT];
    private BlockCapabilityCache<NetworkInterface, Direction>[] adjacentBlockCaches = null;
    private boolean haveAdjacentBlocksChanged = true;
    private final SwitchPacketForwarder packetForwarder = new SwitchPacketForwarder(this);

    public NetworkSwitchBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.NETWORK_SWITCH.get(), pos, state);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void loadServer() {
        super.loadServer();
        final BlockPos pos = getBlockPos();
        final List<BlockCapabilityCache<NetworkInterface, Direction>> adj =
                new ArrayList<>(Constants.BLOCK_FACE_COUNT);
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) adj.add(null);
        for (final Direction side : Constants.DIRECTIONS)
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
            final NetworkInterface source, byte[] frameBytes, final int timeToLive) {
        packetForwarder.forward(source, frameBytes, timeToLive);
    }

    @Override
    public byte[] readEthernetFrame() {
        return new byte[0];
    }

    @Override
    public void clientTick() {}

    @Override
    public void serverTick() {
        if (level == null) return;
        tickCount++;
        if (tickCount % 20 == 1) {
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
        final List<Tag> hosts = tag.getList("hosts", Tag.TAG_COMPOUND);
        hostTable.load(hosts);
        final List<Tag> ports = tag.getList("ports", Tag.TAG_COMPOUND);
        portManager.load(ports);
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
    public void setPortSettings(List<Map<String, ?>> settings) {
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

    Optional<Integer> sideReverseLookup(NetworkInterface iface) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++)
            if (iface.equals(adjacentBlockInterfaces[i])) return Optional.of(i);
        return Optional.empty();
    }

    void validateAdjacentBlocks() {
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
