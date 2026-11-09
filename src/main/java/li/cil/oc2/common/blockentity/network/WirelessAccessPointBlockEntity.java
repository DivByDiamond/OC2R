package li.cil.oc2.common.blockentity.network;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.network.WirelessAccessPointBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.radio.dock.DockBridge;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.platform.CapabilityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** A short-range enrolled access point; only its underside carries wired Ethernet. */
public final class WirelessAccessPointBlockEntity extends ModBlockEntity
        implements NamedDevice, TickableBlockEntity, NetworkInterface {
    private final DockBridge bridge = new DockBridge(
            () -> level instanceof final ServerLevel server ? server : null,
            this::getBlockPos, this::isValid, this, side -> side == Direction.DOWN);

    public WirelessAccessPointBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.WIRELESS_ACCESS_POINT.get(), pos, state);
    }

    @Override
    public void serverTick() {
        if (level == null || level.getGameTime() % 20 != 0) return;
        bridge.refresh();
        final boolean lit = Boolean.TRUE.equals(bridge.status().get("attached"));
        if (getBlockState().getValue(WirelessAccessPointBlock.LIT) != lit)
            level.setBlock(worldPosition, getBlockState().setValue(WirelessAccessPointBlock.LIT, lit), 3);
    }

    public boolean belongsTo(final UUID network) {
        return bridge.belongsTo(network);
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        if (level != null) ServerScheduler.schedule(level, bridge::refresh);
    }

    @Override
    protected void unloadServer(final boolean removed) {
        super.unloadServer(removed);
        bridge.unload();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        bridge.save(tag);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bridge.load(tag);
    }

    @Callback
    public String getRadioEndpointId() {
        return bridge.endpointId();
    }

    @Callback
    public void joinRadioNetwork(@Parameter("invite") final String invite, @Parameter("secret") final String secret) {
        bridge.join(invite, secret);
        setChanged();
    }

    @Callback
    public String getRadioAddress() {
        return bridge.address();
    }

    @Callback
    public Map<String, Object> getRadioStatus() {
        return bridge.status();
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return List.of("wireless_access_point");
    }

    @Override
    public byte[] readEthernetFrame() {
        return new byte[0];
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int ttl) {
        bridge.ethernetIn(frame, ttl);
    }

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> side == Direction.DOWN
                        && be instanceof final WirelessAccessPointBlockEntity self ? self : null,
                Blocks.WIRELESS_ACCESS_POINT.get());
    }
}
