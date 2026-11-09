package li.cil.oc2.common.blockentity.radio.dock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.blockentity.radio.OraEndpoint;
import li.cil.oc2.common.blockentity.radio.RadioNetworkData;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Ethernet bridge of a robot dock station. The dock owns an enrolled client identity like any
 * other wireless client, so a cable alone never joins a radio network: the network administrator
 * has to invite the dock first. Frames from a connector on the dock go over the radio through the
 * nearest tower in range, and frames of the radio network are written to the connectors on the
 * dock. The dock still does not relay between towers.
 */
public final class DockBridge {
    private final Supplier<ServerLevel> level;
    private final Supplier<BlockPos> positionSource;
    private final BooleanSupplier validity;
    private final NetworkInterface self;
    private final java.util.function.Predicate<Direction> cableSides;
    private final BooleanSupplier guestAvailable;
    private final Set<String> services = new LinkedHashSet<>();
    private boolean servicesRestored;
    private UUID host = UUID.randomUUID();
    private final OraEndpoint endpoint;

    /** The dock block entity passes itself as the cable-facing interface, so frames never loop back into it. */
    public DockBridge(
            final Supplier<ServerLevel> level,
            final Supplier<BlockPos> position,
            final BooleanSupplier valid,
            final NetworkInterface self) {
        this(level, position, valid, self, side -> true);
    }

    public DockBridge(
            final Supplier<ServerLevel> level,
            final Supplier<BlockPos> position,
            final BooleanSupplier valid,
            final NetworkInterface self,
            final java.util.function.Predicate<Direction> cableSides) {
        this(level, position, valid, self, cableSides, () -> true);
    }

    /** Infrastructure remains available when the attached guest device bus is stopped. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Keeps the host's live cable interface.")
    public DockBridge(
            final Supplier<ServerLevel> level,
            final Supplier<BlockPos> position,
            final BooleanSupplier valid,
            final NetworkInterface self,
            final java.util.function.Predicate<Direction> cableSides,
            final BooleanSupplier guestAvailable) {
        this.level = level;
        this.positionSource = position;
        this.validity = valid;
        this.self = self;
        this.cableSides = cableSides;
        this.guestAvailable = guestAvailable;
        endpoint = new OraEndpoint(
                level,
                () -> host,
                () -> positionSource.get().getX() + 0.5,
                () -> positionSource.get().getZ() + 0.5,
                this::guestOnline);
    }

    public String endpointId() {
        return endpoint.id().toString();
    }

    public void join(final String invite, final String secret) {
        final ServerLevel server = level.get();
        if (server == null || !validity.getAsBoolean() || !RadioNetworkData.get(server).bridges().canRegister(this)) {
            throw new SecurityException("Access point identity is already held or unavailable");
        }
        endpoint.joinRadioNetwork(invite, secret);
        servicesRestored = false;
        refresh();
    }

    public String address() {
        if (!admitted()) throw new SecurityException("Bridge identity is not admitted");
        return endpoint.address();
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap") // Local builder; only an immutable snapshot escapes.
    public Map<String, Object> status() {
        final Map<String, Object> status = new java.util.HashMap<>(endpoint.status());
        status.put("attached", admitted() && directlyAttached());
        status.put("guestOnline", guestOnline());
        return Map.copyOf(status);
    }

    /** Makes an enrolled dock known to its level so it receives radio frames; harmless otherwise. */
    public void refresh() {
        final ServerLevel server = level.get();
        if (server != null && admitted() && directlyAttached() && guestAvailable.getAsBoolean()) {
            endpoint.tick();
            if (!servicesRestored) {
                for (final String service : services) endpoint.register(service);
                servicesRestored = true;
            }
        } else {
            disconnectGuest();
        }
    }

    public void unload() {
        final ServerLevel server = level.get();
        disconnectGuest();
        if (server != null) RadioNetworkData.get(server).bridges().remove(this);
    }

    private boolean admitted() {
        final ServerLevel server = level.get();
        return server != null && validity.getAsBoolean() && endpoint.enrolled()
                && RadioNetworkData.get(server).bridges().register(this);
    }

    private boolean guestOnline() {
        return guestAvailable.getAsBoolean() && admitted() && directlyAttached();
    }

    private boolean directlyAttached() {
        final ServerLevel server = level.get();
        return server != null && endpoint.enrolled()
                && RadioNetworkData.get(server).bridgeAccess(server, id(), credential(), x(), z()).isPresent();
    }

    /** Server-thread lifecycle cleanup, independent of infrastructure bridge admission. */
    public void disconnectGuest() {
        endpoint.disconnect();
        servicesRestored = false;
    }

    /** Trusted adapter access; copied or unmounted devices cannot inherit a live RPC session. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "The guarded adapter uses the live endpoint.")
    public OraEndpoint authenticatedEndpoint() {
        if (!guestAvailable.getAsBoolean() || !admitted()) {
            throw new SecurityException("Access point guest is not admitted or mounted");
        }
        endpoint.session();
        if (!servicesRestored) {
            for (final String service : services) endpoint.register(service);
            servicesRestored = true;
        }
        return endpoint;
    }

    /** Registers a guest service and remembers its name across block save/load. */
    public void registerService(final String service) {
        authenticatedEndpoint().register(service);
        services.add(service);
    }

    public UUID id() {
        return endpoint.id();
    }

    public boolean valid() {
        return validity.getAsBoolean();
    }

    public BlockPos position() {
        return positionSource.get();
    }

    public boolean enrolled() {
        return validity.getAsBoolean() && endpoint.enrolled();
    }

    public String credential() {
        return endpoint.credential();
    }

    /** Authenticated membership, used to scope an access point to its enrolled network. */
    public boolean belongsTo(final UUID network) {
        final ServerLevel server = level.get();
        return server != null && validity.getAsBoolean()
                && RadioNetworkData.get(server).registry().authenticate(id(), credential())
                        .filter(member -> member.network().equals(network)).isPresent()
                && RadioNetworkData.get(server).bridges().register(this);
    }

    public double x() {
        return endpoint.x();
    }

    public double z() {
        return endpoint.z();
    }

    /** A frame from a connector on the dock. An unenrolled or unattached dock drops it. */
    public void ethernetIn(final byte[] frame, final int ttl) {
        final ServerLevel server = level.get();
        if (server == null || !validity.getAsBoolean() || ttl <= 0 || !validFrame(frame)) return;
        if (!endpoint.enrolled()) return;
        final RadioNetworkData data = RadioNetworkData.get(server);
        if (!data.bridges().register(this) || !directlyAttached()) return;
        data.transmitFromBridge(server, this, frame, ttl);
    }

    /** Writes a radio frame, unchanged, to the connectors on the dock with one hop less TTL. */
    public void ethernetOut(final byte[] frame, final int ttl) {
        final ServerLevel server = level.get();
        if (server == null || ttl <= 0 || !admitted() || !directlyAttached()) return;
        for (final Direction side : Direction.values()) {
            if (!cableSides.test(side)) continue;
            final BlockPos pos = positionSource.get().relative(side);
            if (!server.hasChunkAt(pos)) continue;
            final NetworkInterface iface = Platform.capabilities().getBlockCapability(
                    Capabilities.NetworkInterface.BLOCK, server, pos, null, server.getBlockEntity(pos),
                    side.getOpposite());
            // Skip only the dock itself.
            if (iface != null && iface != self) iface.writeEthernetFrame(self, frame.clone(), ttl - 1); // NOPMD
        }
    }

    static boolean validFrame(final byte[] frame) {
        return frame != null && frame.length >= 14
                && !(frame[12] == (byte) 0x81 && frame[13] == 0 && frame.length < 18);
    }

    public void save(final CompoundTag tag) {
        endpoint.save(tag);
        tag.putString("ora_services", String.join("\n", services));
    }

    public void load(final CompoundTag tag) {
        endpoint.load(tag);
        //? if >=26.1 {
        /*final String saved = tag.getStringOr("ora_owner", "");
        *///?} else {
        final String saved = tag.getString("ora_owner");
        //?}
        //? if >=26.1 {
        /*final String remembered = tag.getStringOr("ora_services", "");
        *///?} else {
        final String remembered = tag.getString("ora_services");
        //?}
        services.clear();
        if (!remembered.isEmpty()) services.addAll(java.util.Arrays.asList(remembered.split("\n")));
        servicesRestored = false;
        try {
            host = UUID.fromString(saved);
        } catch (final IllegalArgumentException missing) {
            host = UUID.randomUUID();
        }
    }
}
