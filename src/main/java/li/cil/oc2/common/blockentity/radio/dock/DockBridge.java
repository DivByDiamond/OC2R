package li.cil.oc2.common.blockentity.radio.dock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
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
    private UUID host = UUID.randomUUID();
    private final OraEndpoint endpoint;

    /** The dock block entity passes itself as the cable-facing interface, so frames never loop back into it. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The dock passes itself as its own cable interface.")
    public DockBridge(
            final Supplier<ServerLevel> level,
            final Supplier<BlockPos> position,
            final BooleanSupplier valid,
            final NetworkInterface self) {
        this.level = level;
        this.positionSource = position;
        this.validity = valid;
        this.self = self;
        endpoint = new OraEndpoint(
                level,
                () -> host,
                () -> positionSource.get().getX() + 0.5,
                () -> positionSource.get().getZ() + 0.5,
                valid);
    }

    public String endpointId() {
        return endpoint.id().toString();
    }

    public void join(final String invite, final String secret) {
        endpoint.joinRadioNetwork(invite, secret);
        refresh();
    }

    public String address() {
        return endpoint.address();
    }

    public Map<String, Object> status() {
        return endpoint.status();
    }

    /** Makes an enrolled dock known to its level so it receives radio frames; harmless otherwise. */
    public void refresh() {
        final ServerLevel server = level.get();
        if (server != null && validity.getAsBoolean() && endpoint.enrolled())
            RadioNetworkData.get(server).bridges().register(this);
    }

    public void unload() {
        final ServerLevel server = level.get();
        if (server != null) RadioNetworkData.get(server).bridges().remove(this);
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
        data.bridges().register(this);
        data.transmitFromBridge(server, this, frame, ttl);
    }

    /** Writes a radio frame, unchanged, to the connectors on the dock with one hop less TTL. */
    public void ethernetOut(final byte[] frame, final int ttl) {
        final ServerLevel server = level.get();
        if (server == null || ttl <= 0) return;
        for (final Direction side : Direction.values()) {
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
    }

    public void load(final CompoundTag tag) {
        endpoint.load(tag);
        //? if >=26.1 {
        /*final String saved = tag.getStringOr("ora_owner", "");
        *///?} else {
        final String saved = tag.getString("ora_owner");
        //?}
        try {
            host = UUID.fromString(saved);
        } catch (final IllegalArgumentException missing) {
            host = UUID.randomUUID();
        }
    }
}
