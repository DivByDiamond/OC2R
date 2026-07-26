/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.element;

import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static li.cil.oc2.common.util.RegistryUtils.optionalKey;

public abstract class AbstractBlockDeviceBusElement extends AbstractGroupingDeviceBusElement<AbstractBlockDeviceBusElement.BlockEntry, BlockDeviceQuery> implements BlockDeviceBusElement {
    public AbstractBlockDeviceBusElement() {
        super(Constants.BLOCK_FACE_COUNT);
    }

    ///////////////////////////////////////////////////////////////////
    // DeviceBusElement

    @Override
    public Optional<Collection<DeviceBusElement>> getNeighbors() {
        final Level commonLevel = getLevel();
        if (commonLevel == null || commonLevel.isClientSide()) {
            return Optional.empty();
        }
        // On Create: Aeronautics contraptions (and any other Valkyrien Skies
        // ship world) the level here is not a ServerLevel. The previous
        // unconditional cast would throw ClassCastException out of the bus
        // scan, the controller would never recover, and the computer would
        // sit in BusState.INCOMPLETE forever — which manifests exactly as
        // "press power, computer appears on, but UART never shows anything".
        // Treat non-ServerLevel levels as having no neighbors so the bus
        // resolves to READY (with just the computer's own devices) instead
        // of getting stuck in INCOMPLETE.
        if (!(commonLevel instanceof final ServerLevel level)) {
            return Optional.of(Collections.emptyList());
        }

        final ArrayList<DeviceBusElement> neighbors = new ArrayList<>();
        for (final Direction neighborDirection : Constants.DIRECTIONS) {
            if (!canScanContinueTowards(neighborDirection)) {
                continue;
            }

            final BlockPos neighborPos = getPosition().relative(neighborDirection);

            final ChunkPos chunkPos = new ChunkPos(neighborPos);
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                return Optional.empty();
            }

            final BlockEntity blockEntity = level.getBlockEntity(neighborPos);
            if (blockEntity == null) {
                continue;
            }

            final DeviceBusElement capability =
                level.getCapability(
                    Capabilities.DeviceBusElement.BLOCK,
                    neighborPos,
                    neighborDirection.getOpposite()
                );

            if (capability != null) {
                neighbors.add(capability);
            }
        }

        return Optional.of(neighbors);
    }

    ///////////////////////////////////////////////////////////////////

    public void updateDevicesForNeighbor(final Direction side) {
        final LevelAccessor level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        final var registries = level.registryAccess();

        final int index = side.get3DDataValue();
        collectDevices(level, getPosition().relative(side), side).ifPresentOrElse(
            queryResult -> setEntriesForGroup(registries, index, queryResult),
            () -> setEntriesForGroupUnloaded(registries, index)
        );
    }

    public void setRemoved() {
        final LevelAccessor level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        final var registries = level.registryAccess();

        for (final Direction side : Direction.values()) {
            final int index = side.get3DDataValue();
            final BlockPos pos = getPosition().relative(side);
            final BlockDeviceQuery query = Devices.makeQuery(level, pos, side.getOpposite());
            setEntriesForGroup(registries, index, new BlockQueryResult(query, Collections.emptySet()));
        }

        scheduleScan();
    }

    ///////////////////////////////////////////////////////////////////

    protected boolean canScanContinueTowards(@Nullable final Direction direction) {
        return true;
    }

    protected boolean canDetectDevicesTowards(@Nullable final Direction direction) {
        return canScanContinueTowards(direction);
    }

    protected Optional<BlockQueryResult> collectDevices(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side) {
        final BlockDeviceQuery query = Devices.makeQuery(level, pos, side != null ? side.getOpposite() : null);
        final HashSet<BlockEntry> entries = new HashSet<>();

        if (canDetectDevicesTowards(side)) {
            final Optional<List<BlockDeviceInfo>> loadedDevices = Devices.getDevices(query);
            if (loadedDevices.isPresent()) {
                for (final BlockDeviceInfo blockDeviceInfo : loadedDevices.get()) {
                    entries.add(new BlockEntry(blockDeviceInfo, side));
                }
            } else {
                return Optional.empty();
            }

            collectSyntheticDevices(level, pos, side, entries);
        }

        return Optional.of(new BlockQueryResult(query, entries));
    }

    protected void collectSyntheticDevices(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side, final HashSet<BlockEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        final String blockName = LevelUtils.getBlockName(level, pos);
        if (blockName != null) {
            entries.add(new BlockEntry(new BlockDeviceInfo(null, new TypeNameRPCDevice(blockName)), side));
        }
    }

    @Override
    protected void onEntryRemoved(final String dataKey, final CompoundTag tag, @Nullable final BlockDeviceQuery query) {
        super.onEntryRemoved(dataKey, tag, query);
        assert query != null : "Passed null query for block device bus element.";
        final Registry<BlockDeviceProvider> registry = Providers.blockDeviceProviderRegistry();
        final BlockDeviceProvider provider = registry.get(ResourceLocation.parse(dataKey));
        if (provider != null) {
            provider.unmount(query, tag);
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected final class BlockQueryResult extends GroupQueryResult<BlockEntry, BlockDeviceQuery> {
        private final BlockDeviceQuery query;
        private final Set<BlockEntry> entries;

        public BlockQueryResult(final BlockDeviceQuery query, final Set<BlockEntry> entries) {
            this.query = query;
            this.entries = entries;
        }

        public BlockDeviceQuery getQuery() {
            return query;
        }

        @Override
        public Set<BlockEntry> getEntries() {
            return entries;
        }
    }

    protected static final class BlockEntry implements GroupEntry {
        private final BlockDeviceInfo deviceInfo;
        @Nullable private final String dataKey;
        private final Device device;
        @Nullable private final Direction side;

        public BlockEntry(final BlockDeviceInfo deviceInfo, @Nullable final Direction side) {
            this.deviceInfo = deviceInfo;
            this.side = side;

            // Grab these while the device info has not yet been invalidated. We still need to access
            // these even after the device has been invalidated to clean up.
            this.dataKey = optionalKey(deviceInfo.provider).orElse(null);
            this.device = deviceInfo.device;
        }

        @Override
        public Optional<String> getDeviceDataKey() {
            return Optional.ofNullable(dataKey);
        }

        @Override
        public Optional<String> getLegacyDeviceDataKey() {
            if (dataKey != null) {
                return Optional.of("oc2r:block_device_provider");
            }
            return Optional.empty();
        }

        @Override
        public int getDeviceEnergyConsumption() {
            return deviceInfo.getEnergyConsumption();
        }

        @Override
        public Device getDevice() {
            return device;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final BlockEntry that = (BlockEntry) o;
            return Objects.equals(dataKey, that.dataKey) && device.equals(that.device) && side == that.side;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataKey, device, side);
        }

        @Override
        public String toString() {
            return device.toString();
        }
    }
}
