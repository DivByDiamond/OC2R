package li.cil.oc2.common.blockentity.network;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.LifecycleAwareDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.network.WirelessAccessPointBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.blockentity.radio.OraEndpoint;
import li.cil.oc2.common.blockentity.radio.dock.DockBridge;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.radio.service.RadioMessage;
import li.cil.oc2.common.radio.service.RadioServiceBroker;
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
        implements NamedDevice, TickableBlockEntity, NetworkInterface, LifecycleAwareDevice {
    private final AtomicInteger activeMounts = new AtomicInteger();
    private final AtomicBoolean disconnectPending = new AtomicBoolean();
    private final DockBridge bridge = new DockBridge(
            () -> level instanceof final ServerLevel server ? server : null,
            this::getBlockPos, this::isValid, this, side -> side == Direction.DOWN,
            () -> activeMounts.get() > 0 && !disconnectPending.get());

    public WirelessAccessPointBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.WIRELESS_ACCESS_POINT.get(), pos, state);
    }

    @Override
    public void serverTick() {
        if (level == null || !isValid()) return;
        drainGuestDisconnect();
        bridge.refresh();
        final boolean lit = Boolean.TRUE.equals(bridge.status().get("attached"));
        if (getBlockState().getValue(WirelessAccessPointBlock.LIT) != lit)
            level.setBlock(worldPosition, getBlockState().setValue(WirelessAccessPointBlock.LIT, lit), 3);
    }

    @Override
    public void onDeviceMounted() {
        activeMounts.incrementAndGet();
    }

    @Override
    public void onDeviceUnmounted() {
        if (activeMounts.updateAndGet(count -> Math.max(0, count - 1)) == 0) disconnectPending.set(true);
    }

    private void drainGuestDisconnect() {
        if (disconnectPending.getAndSet(false)) bridge.disconnectGuest();
    }

    private OraEndpoint radio() {
        drainGuestDisconnect();
        return bridge.authenticatedEndpoint();
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
        return radio().address();
    }

    @Callback
    public Map<String, Object> getRadioStatus() {
        return bridge.status();
    }

    @Callback
    public void registerRadioService(@Parameter("service") final String service) {
        radio();
        bridge.registerService(service);
        setChanged();
    }

    @Callback
    public void sendRadioMessage(
            @Parameter("address") final String address,
            @Parameter("service") final String service,
            @Parameter("channel") final String channel,
            @Parameter("requestId") final String request,
            @Parameter("payload") final String payload) {
        radio().send(address, service, channel, request, payload);
    }

    @Callback
    public List<RadioMessage> pollRadioMessages() {
        return radio().pollMessages();
    }

    @Callback
    public void publishRadioTelemetry(
            @Parameter("service") final String service,
            @Parameter("channel") final String channel,
            @Parameter("payload") final String payload) {
        radio().publish(service, channel, payload);
    }

    @Callback
    public String subscribeRadioTelemetry(
            @Parameter("address") final String address,
            @Parameter("service") final String service,
            @Parameter("fields") final String[] fields,
            @Parameter("staleTicks") final int staleTicks) {
        return radio().subscribe(address, service, fields, staleTicks);
    }

    @Callback
    public void unsubscribeRadioTelemetry(@Parameter("subscription") final String subscription) {
        radio().unsubscribe(subscription);
    }

    @Callback
    public String sendRadioCommand(
            @Parameter("address") final String address,
            @Parameter("service") final String service,
            @Parameter("channel") final String channel,
            @Parameter("requestId") final String request,
            @Parameter("payload") final String payload,
            @Parameter("timeoutTicks") final int timeout) {
        return radio().command(address, service, channel, request, payload, timeout);
    }

    @Callback
    public void updateRadioCommand(
            @Parameter("requestId") final String request,
            @Parameter("state") final String state,
            @Parameter("result") final String result) {
        radio().commandStatus(request, state, result);
    }

    @Callback
    public List<String> discoverRadioServices(@Parameter("address") final String address) {
        return radio().discover(address);
    }

    @Callback
    public List<RadioServiceBroker.Peer> listRadioDevices() {
        return radio().peers();
    }

    @Callback
    public RadioServiceBroker.Estimate estimateRadioRoute(@Parameter("address") final String address) {
        return radio().estimate(address);
    }

    @Callback
    public void pingRadio(@Parameter("address") final String address, @Parameter("requestId") final String request) {
        radio().ping(address, request);
    }

    @Callback
    public List<RadioServiceBroker.Event> pollRadioEvents() {
        return radio().poll();
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return List.of("wireless_access_point", "gps_tower");
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
