package li.cil.oc2.common.blockentity.gps;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.block.gps.GpsTowerStructure;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** A wired RPC endpoint and an explicitly paired radio navigation station. */
public final class GpsTowerBlockEntity extends ModBlockEntity implements NamedDevice {
    private UUID stationId = UUID.randomUUID();
    private UUID networkId = UUID.randomUUID();
    private boolean reference;

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
        final BlockPos position = Config.relativeCoordinates
                ? data().network().offset(stationId, Config.gpsTowerRange) : worldPosition;
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
        final UUID selected;
        try {
            selected = UUID.fromString(id);
        } catch (final IllegalArgumentException | NullPointerException invalid) {
            throw new IllegalArgumentException("Invalid GPS network ID");
        }
        final GpsNetworkData data = data();
        if (selected.equals(networkId)) return;
        if (reference) throw new IllegalStateException("Clear reference before changing GPS network");
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
        if (level instanceof final ServerLevel server && isAssembled()) {
            GpsNetworkData.get(server).register(node());
        }
    }

    boolean matches(final GpsNetworkNode node) {
        return stationId.equals(node.id()) && networkId.equals(node.network()) && reference == node.reference();
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
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        //? if >=26.1 {
        /*stationId = readId(tag.getStringOr("gps_station", ""), stationId);
        networkId = readId(tag.getStringOr("gps_network", ""), networkId);
        reference = tag.getBooleanOr("gps_reference", false);
        *///?} else {
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
        if (!(level instanceof final ServerLevel server)) throw new IllegalStateException("GPS requires a server level");
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
        if (!Config.relativeCoordinates) throw new IllegalStateException("Relative coordinates are disabled");
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return List.of("gps_tower");
    }
}
