package li.cil.oc2.common.blockentity.gps;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.gps.GpsTowerStructure;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.radio.RadioNetworkData;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.radio.RadioTopology;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.platform.CapabilityRegistrar;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** A wired RPC endpoint and an explicitly paired radio navigation station. */
@SuppressWarnings("PMD.CyclomaticComplexity") // Many small RPC callbacks, each with its own guards.
public final class GpsTowerBlockEntity extends ModBlockEntity
        implements NamedDevice, NetworkInterface {
    private UUID stationId = UUID.randomUUID();
    private UUID networkId = UUID.randomUUID();
    private boolean reference;
    private UUID radioEndpoint = UUID.randomUUID();
    private String radioCredential = "";
    private final li.cil.oc2.common.blockentity.radio.OraEndpoint applicationEndpoint =
            new li.cil.oc2.common.blockentity.radio.OraEndpoint(
                    () -> (ServerLevel) level,
                    () -> radioEndpoint,
                    worldPosition::getX,
                    worldPosition::getZ,
                    () -> !isRemoved() && isAssembled());

    private li.cil.oc2.common.blockentity.radio.OraEndpoint ora() {
        applicationEndpoint.identity(radioEndpoint, radioCredential);
        return applicationEndpoint;
    }

    @Callback
    public void registerRadioService(@Parameter("service") String service) {
        ora().register(service);
    }

    @Callback
    public void sendRadioMessage(
            @Parameter("address") String address,
            @Parameter("service") String service,
            @Parameter("channel") String channel,
            @Parameter("requestId") String request,
            @Parameter("payload") String payload) {
        ora().send(address, service, channel, request, payload);
    }

    @Callback
    public java.util.List<li.cil.oc2.common.radio.service.RadioMessage> pollRadioMessages() {
        return ora().pollMessages();
    }

    @Callback
    public void publishRadioTelemetry(
            @Parameter("service") String service,
            @Parameter("channel") String channel,
            @Parameter("payload") String payload) {
        ora().publish(service, channel, payload);
    }

    @Callback
    public String subscribeRadioTelemetry(
            @Parameter("address") String address,
            @Parameter("service") String service,
            @Parameter("fields") String[] fields,
            @Parameter("staleTicks") int staleTicks) {
        return ora().subscribe(address, service, fields, staleTicks);
    }

    /** Per field sample age (ticks and nominal milliseconds) with online, stale or offline state. */
    @Callback
    public java.util.List<li.cil.oc2.common.radio.service.RadioServiceBroker.FieldStatus>
            getRadioTelemetryStatus(@Parameter("subscription") String subscription) {
        return ora().telemetryStatus(subscription);
    }

    @Callback
    public void unsubscribeRadioTelemetry(@Parameter("subscription") String subscription) {
        ora().unsubscribe(subscription);
    }

    @Callback
    public String sendRadioCommand(
            @Parameter("address") String address,
            @Parameter("service") String service,
            @Parameter("channel") String channel,
            @Parameter("requestId") String request,
            @Parameter("payload") String payload,
            @Parameter("timeoutTicks") int timeout) {
        return ora().command(address, service, channel, request, payload, timeout);
    }

    @Callback
    public void updateRadioCommand(
            @Parameter("requestId") String request,
            @Parameter("state") String state,
            @Parameter("result") String result) {
        ora().commandStatus(request, state, result);
    }

    @Callback
    public java.util.List<String> discoverRadioServices(@Parameter("address") String address) {
        return ora().discover(address);
    }

    @Callback
    public java.util.List<li.cil.oc2.common.radio.service.RadioServiceBroker.Peer> listRadioDevices() {
        return ora().peers();
    }

    @Callback
    public li.cil.oc2.common.radio.service.RadioServiceBroker.Estimate estimateRadioRoute(
            @Parameter("address") String address) {
        return ora().estimate(address);
    }

    @Callback
    public void pingRadio(
            @Parameter("address") String address, @Parameter("requestId") String request) {
        ora().ping(address, request);
    }

    @Callback
    public java.util.List<li.cil.oc2.common.radio.service.RadioServiceBroker.Event>
            pollRadioEvents() {
        return ora().poll();
    }

    @Callback
    public String getRadioEndpointId() {
        return radioEndpoint.toString();
    }

    /**
     * Bootstrap authority is possession of the local physical wired RPC endpoint. Returned once.
     */
    @Callback
    public Map<String, String> createRadioNetwork() {
        requireAssembled();
        final var data = radioData();
        if (data.registry().member(radioEndpoint).isPresent())
            throw new IllegalStateException("Tower already enrolled");
        final var created = data.registry().createNetwork();
        final var invite =
                data.registry()
                        .invite(
                                created.network(),
                                created.adminSecret(),
                                getDimension(),
                                radioEndpoint,
                                java.util.Arrays.stream(
                                                li.cil.oc2.common.radio.RadioRegistry.Permission
                                                        .values())
                                        .map(
                                                permission ->
                                                        new li.cil.oc2.common.radio.RadioRegistry
                                                                .Scope(permission, null, null))
                                        .collect(java.util.stream.Collectors.toSet()),
                                level.getGameTime(),
                                Config.radioInviteLifetimeTicks);
        radioCredential =
                data.registry()
                        .enroll(invite.invite(), invite.secret(), level.getGameTime())
                        .secret();
        setChanged();
        data.setDirty();
        refreshRadio();
        return Map.of(
                "network", created.network().toString(), "adminSecret", created.adminSecret());
    }

    @Callback
    public Map<String, String> issueRadioInvite(
            @Parameter("adminSecret") final String adminSecret,
            @Parameter("endpointId") final String endpointId) {
        requireAssembled();
        final var data = radioData();
        final var member =
                data.registry()
                        .authenticate(radioEndpoint, radioCredential)
                        .orElseThrow(() -> new SecurityException("Tower is not enrolled"));
        final var invite =
                data.registry()
                        .invite(
                                member.network(),
                                adminSecret,
                                getDimension(),
                                UUID.fromString(endpointId),
                                Set.of(),
                                level.getGameTime(),
                                Config.radioInviteLifetimeTicks);
        data.setDirty();
        return Map.of("invite", invite.invite().toString(), "secret", invite.secret());
    }

    @Callback
    public Map<String, String> issueRadioInviteWithPermissions(
            @Parameter("adminSecret") String adminSecret,
            @Parameter("endpointId") String endpointId,
            @Parameter("permissions") String[] permissions,
            @Parameter("targetEndpointId") String target,
            @Parameter("service") String service) {
        requireAssembled();
        var data = radioData();
        var member =
                data.registry()
                        .authenticate(radioEndpoint, radioCredential)
                        .orElseThrow(SecurityException::new);
        var invite =
                data.registry()
                        .invite(
                                member.network(),
                                adminSecret,
                                getDimension(),
                                UUID.fromString(endpointId),
                                scopes(permissions, target, service),
                                level.getGameTime(),
                                Config.radioInviteLifetimeTicks);
        data.setDirty();
        return Map.of("invite", invite.invite().toString(), "secret", invite.secret());
    }

    @Callback
    public void setRadioPermissions(
            @Parameter("adminSecret") String adminSecret,
            @Parameter("endpointId") String endpointId,
            @Parameter("permissions") String[] permissions,
            @Parameter("targetEndpointId") String target,
            @Parameter("service") String service) {
        requireAssembled();
        var data = radioData();
        var member =
                data.registry()
                        .authenticate(radioEndpoint, radioCredential)
                        .orElseThrow(SecurityException::new);
        data.registry()
                .setScopes(
                        member.network(),
                        adminSecret,
                        UUID.fromString(endpointId),
                        scopes(permissions, target, service));
        data.setDirty();
    }

    private Set<li.cil.oc2.common.radio.RadioRegistry.Scope> scopes(
            String[] permissions, String target, String service) {
        UUID device =
                target == null || target.isEmpty() || "*".equals(target)
                        ? null
                        : UUID.fromString(target);
        String name = service == null || service.isEmpty() || "*".equals(service) ? null : service;
        if (device != null) {
            var data = radioData();
            var own =
                    data.registry()
                            .authenticate(radioEndpoint, radioCredential)
                            .orElseThrow(SecurityException::new);
            var destination = data.registry().member(device).orElseThrow(SecurityException::new);
            if (!destination.network().equals(own.network())
                    || !destination.dimension().equals(getDimension()))
                throw new SecurityException("Scope target belongs to another network");
        }
        return java.util.Arrays.stream(permissions)
                .map(
                        value ->
                                new li.cil.oc2.common.radio.RadioRegistry.Scope(
                                        li.cil.oc2.common.radio.RadioRegistry.Permission.valueOf(
                                                value),
                                        device,
                                        name))
                .collect(java.util.stream.Collectors.toSet());
    }

    /** Pending enrollment as shown to an administrator; scopes use {@link #describe}. */
    public record PendingEnrollment(
            String request, String endpointId, java.util.List<String> scopes, long expiresAtTick) {
        public PendingEnrollment {
            scopes = java.util.List.copyOf(scopes);
        }
    }

    /**
     * Like {@link #issueRadioInviteWithPermissions} but the invitation only opens a pending
     * request; an administrator must approve it with {@link #approveEnrollment}.
     */
    @Callback
    public Map<String, String> issueRadioInviteForApproval(
            @Parameter("adminSecret") String adminSecret,
            @Parameter("endpointId") String endpointId,
            @Parameter("permissions") String[] permissions,
            @Parameter("targetEndpointId") String target,
            @Parameter("service") String service) {
        requireAssembled();
        final var admin = requireAdmin();
        final var data = radioData();
        final var invite =
                data.registry()
                        .invite(
                                admin.network(),
                                adminSecret,
                                getDimension(),
                                UUID.fromString(endpointId),
                                scopes(permissions, target, service),
                                level.getGameTime(),
                                Config.radioInviteLifetimeTicks,
                                true);
        data.setDirty();
        return Map.of("invite", invite.invite().toString(), "secret", invite.secret());
    }

    /**
     * Presents an approval-required invitation. Grants nothing until approved; returns the request
     * id. Once approved, call {@link #completeRadioEnrollment} to join the radio topology.
     */
    @Callback
    public String requestRadioEnrollment(
            @Parameter("invite") final String invitation,
            @Parameter("secret") final String secret) {
        requireAssembled();
        final var data = radioData();
        final UUID id = UUID.fromString(invitation);
        final var invite =
                data.registry().exportState().invites().stream()
                        .filter(value -> value.id().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new SecurityException("Invalid invitation"));
        if (!invite.device().equals(radioEndpoint) || !invite.dimension().equals(getDimension()))
            throw new SecurityException("Invitation belongs to another endpoint");
        final var ticket = data.registry().requestEnrollment(id, secret, level.getGameTime());
        radioCredential = ticket.secret();
        setChanged();
        data.setDirty();
        return ticket.request().toString();
    }

    /** True once a pending request was approved; registers this tower in the radio topology. */
    @Callback
    public boolean completeRadioEnrollment() {
        requireAssembled();
        return refreshRadio();
    }

    @Callback
    public java.util.List<PendingEnrollment> listPendingEnrollments(
            @Parameter("adminSecret") final String adminSecret) {
        requireAssembled();
        final var admin = requireAdmin();
        return radioData()
                .registry()
                .pendingEnrollments(admin.network(), adminSecret, level.getGameTime())
                .stream()
                .map(
                        request ->
                                new PendingEnrollment(
                                        request.id().toString(),
                                        request.device().toString(),
                                        request.requestedScopes().stream()
                                                .map(GpsTowerBlockEntity::describe)
                                                .sorted()
                                                .toList(),
                                        request.expiresAtTick()))
                .toList();
    }

    /**
     * Approves with exactly the listed scopes, each either a bare permission name (all requested
     * scopes of that permission) or a full description from the pending list. An empty list admits
     * the device with no permissions; names that were not requested are rejected.
     */
    @Callback
    public void approveEnrollment(
            @Parameter("adminSecret") final String adminSecret,
            @Parameter("requestId") final String requestId,
            @Parameter("scopes") final String... scopes) {
        requireAssembled();
        final var admin = requireAdmin();
        final var data = radioData();
        final UUID id = UUID.fromString(requestId);
        final var request =
                data.registry()
                        .pendingEnrollments(admin.network(), adminSecret, level.getGameTime())
                        .stream()
                        .filter(value -> value.id().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new SecurityException("Unknown enrollment request"));
        final Set<li.cil.oc2.common.radio.RadioRegistry.Scope> approved = new java.util.HashSet<>();
        for (final String name : scopes) {
            final var matched =
                    request.requestedScopes().stream()
                            .filter(
                                    scope ->
                                            describe(scope).equals(name)
                                                    || scope.permission().name().equals(name))
                            .toList();
            if (matched.isEmpty())
                throw new SecurityException("Scope was not requested: " + name);
            approved.addAll(matched);
        }
        data.registry()
                .approveEnrollment(admin.network(), adminSecret, id, approved, level.getGameTime());
        data.setDirty();
    }

    @Callback
    public void denyEnrollment(
            @Parameter("adminSecret") final String adminSecret,
            @Parameter("requestId") final String requestId) {
        requireAssembled();
        final var admin = requireAdmin();
        final var data = radioData();
        data.registry()
                .denyEnrollment(
                        admin.network(), adminSecret, UUID.fromString(requestId), level.getGameTime());
        data.setDirty();
    }

    /** The tower must itself be a live member holding NETWORK_ADMIN; checked on every call. */
    private li.cil.oc2.common.radio.RadioRegistry.Member requireAdmin() {
        final var registry = radioData().registry();
        final var member =
                registry.authenticate(radioEndpoint, radioCredential)
                        .orElseThrow(() -> new SecurityException("Tower is not enrolled"));
        if (!registry.authorized(
                radioEndpoint,
                radioCredential,
                li.cil.oc2.common.radio.RadioRegistry.Permission.NETWORK_ADMIN,
                null,
                null)) throw new SecurityException("Tower lacks network administration");
        return member;
    }

    private static String describe(final li.cil.oc2.common.radio.RadioRegistry.Scope scope) {
        return scope.device() == null && scope.service() == null
                ? scope.permission().name()
                : scope.permission().name()
                        + ":"
                        + (scope.device() == null ? "*" : scope.device())
                        + ":"
                        + (scope.service() == null ? "*" : scope.service());
    }

    @Callback
    public void joinRadioNetwork(
            @Parameter("invite") final String invitation,
            @Parameter("secret") final String secret) {
        requireAssembled();
        final var data = radioData();
        final UUID id = UUID.fromString(invitation);
        final var invite =
                data.registry().exportState().invites().stream()
                        .filter(value -> value.id().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new SecurityException("Invalid invitation"));
        if (!invite.device().equals(radioEndpoint) || !invite.dimension().equals(getDimension()))
            throw new SecurityException("Invitation belongs to another endpoint");
        radioCredential = data.registry().enroll(id, secret, level.getGameTime()).secret();
        setChanged();
        data.setDirty();
        refreshRadio();
    }

    @Callback
    public void revokeRadioEndpoint(
            @Parameter("adminSecret") final String adminSecret,
            @Parameter("endpointId") final String endpointId) {
        requireAssembled();
        final var data = radioData();
        final var member =
                data.registry()
                        .authenticate(radioEndpoint, radioCredential)
                        .orElseThrow(() -> new SecurityException("Tower is not enrolled"));
        data.registry().revoke(member.network(), adminSecret, UUID.fromString(endpointId));
        data.setDirty();
    }

    @Callback
    public String getRadioAddress() {
        requireAssembled();
        final var data = radioData();
        if (data.registry().authenticate(radioEndpoint, radioCredential).isEmpty())
            throw new SecurityException("Tower is not enrolled");
        return data.registry().address(radioEndpoint).toString();
    }

    @Callback
    public Map<String, Object> getRadioStatus() {
        return ora().status();
    }

    @Override
    public byte[] readEthernetFrame() {
        return new byte[0];
    }

    @Override
    public void writeEthernetFrame(
            final NetworkInterface source, final byte[] frame, final int ttl) {
        if (level instanceof final ServerLevel server && isAssembled() && ttl > 0) {
            if (frame == null
                    || frame.length < 14
                    || (frame[12] == (byte) 0x81 && frame[13] == 0 && frame.length < 18)) return;
            if (refreshRadio())
                radioData().transmit(server, radioEndpoint, radioCredential, frame, ttl);
        }
    }

    /**
     * Delivers unchanged Ethernet bytes to loaded local cable interfaces, retaining the remaining
     * hop TTL.
     */
    public void receiveRadioFrame(final byte[] frame, final int ttl) {
        if (!(level instanceof ServerLevel) || !isAssembled() || ttl <= 0) return;
        for (final Direction side : Direction.values()) {
            final BlockPos pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos)) continue;
            final var iface =
                    Platform.capabilities()
                            .getBlockCapability(
                                    Capabilities.NetworkInterface.BLOCK,
                                    level,
                                    pos,
                                    null,
                                    level.getBlockEntity(pos),
                                    side.getOpposite());
            if (iface != null && iface != this) // NOPMD - skip only this exact block entity
                iface.writeEthernetFrame(this, frame.clone(), ttl - 1);
        }
    }

    public boolean matchesRadio(final RadioTopology.Node node) {
        return radioEndpoint.equals(node.device())
                && worldPosition.equals(BlockPos.containing(node.x(), node.y(), node.z()))
                && isAssembled()
                && radioData()
                        .registry()
                        .authenticate(radioEndpoint, radioCredential)
                        .filter(
                                member ->
                                        member.network().equals(node.network())
                                                && member.dimension().equals(getDimension()))
                        .isPresent();
    }

    private RadioNetworkData radioData() {
        if (!(level instanceof final ServerLevel server))
            throw new IllegalStateException("Radio requires a server level");
        return RadioNetworkData.get(server);
    }

    private boolean refreshRadio() {
        if (!(level instanceof ServerLevel) || !isAssembled()) return false;
        final var data = radioData();
        final boolean registered =
                data.registry()
                        .authenticate(radioEndpoint, radioCredential)
                        .map(
                                member ->
                                        data.register(
                                                new RadioTopology.Node(
                                                        radioEndpoint,
                                                        member.network(),
                                                        getDimension(),
                                                        worldPosition.getX(),
                                                        worldPosition.getY(),
                                                        worldPosition.getZ(),
                                                        Config.gpsTowerRange,
                                                        true)))
                        .orElse(false);
        if (registered) ora().tick();
        return registered;
    }

    public static void registerRadioCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) ->
                        be instanceof final GpsTowerBlockEntity self && self.isAssembled()
                                ? self
                                : null,
                Blocks.GPS_TOWER.get());
    }

    @Override
    protected void unloadServer(final boolean removed) {
        super.unloadServer(removed);
        if (level instanceof ServerLevel) {
            if (removed) radioData().removeAt(worldPosition);
            else radioData().offline(radioEndpoint);
        }
    }

    public GpsTowerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.GPS_TOWER.get(), pos, state);
    }

    @Callback
    public boolean isAssembled() {
        return level != null && isValid() && GpsTowerStructure.isComplete(level, worldPosition);
    }

    /** Reports world XYZ or a radio-network offset, according to server config. */
    @Callback
    public int[] getPosition() {
        requireAssembled();
        final BlockPos position =
                Config.relativeCoordinates
                        ? data().network().offset(stationId, Config.gpsTowerRange)
                        : worldPosition;
        return new int[] {position.getX(), position.getY(), position.getZ()};
    }

    @Callback
    public String getId() {
        return stationId.toString();
    }

    @Callback
    public String getNetworkId() {
        return networkId.toString();
    }

    @Callback
    public int getRange() {
        return Config.gpsTowerRange;
    }

    @Callback
    public boolean isReference() {
        return reference && isAssembled();
    }

    /** No automatic election: an existing reference must be explicitly cleared first. */
    @Callback
    public void setAsReference() {
        requireRelativeMode();
        requireAssembled();
        final GpsNetworkData data = data();
        if (reference) return;
        if (data.network().hasReference(networkId)) {
            throw new IllegalStateException("GPS network already has a reference");
        }
        reference = true;
        membershipChanged(data);
    }

    @Callback
    public void clearReference() {
        requireRelativeMode();
        requireAssembled();
        reference = false;
        membershipChanged(data());
    }

    /** Pairing IDs carry no world coordinates; distant or disconnected stations get no signal. */
    @Callback
    public void joinNetwork(@Parameter("networkId") final String id) {
        requireRelativeMode();
        requireAssembled();
        if (id == null) throw new IllegalArgumentException("Invalid GPS network ID");
        final UUID selected;
        try {
            selected = UUID.fromString(id);
        } catch (final IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Invalid GPS network ID", invalid);
        }
        final GpsNetworkData data = data();
        if (selected.equals(networkId)) return;
        if (reference)
            throw new IllegalStateException("Clear reference before changing GPS network");
        data.network().requireReference(selected);
        networkId = selected;
        membershipChanged(data);
    }

    @Callback
    public boolean hasSignal() {
        if (!isAssembled()) return false;
        if (!Config.relativeCoordinates) return true;
        try {
            data().network().offset(stationId, Config.gpsTowerRange);
            return true;
        } catch (final IllegalStateException unavailable) {
            return false;
        }
    }

    @Callback
    public String getDimension() {
        requireAssembled();
        //? if >=26.1 {
        /*return level.dimension().identifier().toString();
         *///?} else {
        return level.dimension().location().toString();
        //?}
    }

    /** Called after the multiblock has been placed, and when its chunk loads. */
    public void refreshNetwork() {
        refreshRadio();
        if (level instanceof final ServerLevel server && isAssembled()) {
            GpsNetworkData.get(server).register(node());
        }
    }

    boolean matches(final GpsNetworkNode node) {
        return stationId.equals(node.id())
                && networkId.equals(node.network())
                && reference == node.reference();
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        if (level != null) ServerScheduler.schedule(level, this::refreshNetwork);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("gps_station", stationId.toString());
        tag.putString("gps_network", networkId.toString());
        tag.putBoolean("gps_reference", reference);
        tag.putString("radio_endpoint", radioEndpoint.toString());
        tag.putString("radio_credential", radioCredential);
        ora().save(tag);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        applicationEndpoint.load(tag);
        //? if >=26.1 {
        /*radioEndpoint = readId(tag.getStringOr("radio_endpoint", ""), radioEndpoint);
        radioCredential = tag.getStringOr("radio_credential", "");
        stationId = readId(tag.getStringOr("gps_station", ""), stationId);
        networkId = readId(tag.getStringOr("gps_network", ""), networkId);
        reference = tag.getBooleanOr("gps_reference", false);
        *///?} else {
        radioEndpoint = readId(tag.getString("radio_endpoint"), radioEndpoint);
        radioCredential = tag.getString("radio_credential");
        stationId = readId(tag.getString("gps_station"), stationId);
        networkId = readId(tag.getString("gps_network"), networkId);
        reference = tag.getBoolean("gps_reference");
        //?}
    }

    private static UUID readId(final String value, final UUID fallback) {
        if (value.isEmpty()) return fallback;
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException invalid) {
            return fallback;
        }
    }

    private GpsNetworkNode node() {
        return new GpsNetworkNode(stationId, worldPosition, networkId, reference);
    }

    private GpsNetworkData data() {
        if (!(level instanceof final ServerLevel server))
            throw new IllegalStateException("GPS requires a server level");
        final GpsNetworkData data = GpsNetworkData.get(server);
        data.reconcile(server);
        data.register(node());
        return data;
    }

    private void membershipChanged(final GpsNetworkData data) {
        setChanged();
        data.register(node());
    }

    private void requireAssembled() {
        if (!isAssembled()) throw new IllegalStateException("GPS tower is incomplete");
    }

    private static void requireRelativeMode() {
        if (!Config.relativeCoordinates)
            throw new IllegalStateException("Relative coordinates are disabled");
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return List.of("gps_tower");
    }
}
