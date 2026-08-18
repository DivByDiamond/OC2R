package li.cil.oc2.common.bus.element;

import java.util.*;
import li.cil.oc2.api.bus.BlockDeviceBusElement;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.bus.device.util.info.BlockDeviceInfo;
import li.cil.oc2.common.bus.element.group.query.BlockEntry;
import li.cil.oc2.common.bus.element.group.query.BlockQueryResult;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.world.level.LevelUtils;
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

public abstract class AbstractBlockDeviceBusElement
        extends AbstractGroupingDeviceBusElement<BlockEntry, BlockDeviceQuery>
        implements BlockDeviceBusElement {
    public AbstractBlockDeviceBusElement() {
        super(Constants.BLOCK_FACE_COUNT);
    }

    @Override
    public Optional<Collection<DeviceBusElement>> getNeighbors() {
        final Level commonLevel = getLevel();
        if (commonLevel == null || commonLevel.isClientSide()) {
            return Optional.empty();
        }
        if (!(commonLevel instanceof final ServerLevel level)) {
            return Optional.of(Collections.emptyList());
        }

        final List<DeviceBusElement> neighbors = new ArrayList<>();
        for (final Direction neighborDirection : Constants.DIRECTIONS) {
            if (!canScanContinueTowards(neighborDirection)) {
                continue;
            }

            final BlockPos neighborPos = getPosition().relative(neighborDirection);

            final ChunkPos chunkPos =
                    new ChunkPos(neighborPos); // NOPMD: depends on loop direction
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
                            neighborDirection.getOpposite());

            if (capability != null) {
                neighbors.add(capability);
            }
        }

        return Optional.of(neighbors);
    }

    public void updateDevicesForNeighbor(final Direction side) {
        final LevelAccessor level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        final var registries = level.registryAccess();

        final int index = side.get3DDataValue();
        collectDevices(level, getPosition().relative(side), side)
                .ifPresentOrElse(
                        queryResult -> setEntriesForGroup(registries, index, queryResult),
                        () -> setEntriesForGroupUnloaded(registries, index));
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
            setEntriesForGroup(
                    registries,
                    index,
                    new BlockQueryResult(// NOPMD: depends on loop side
                            query, Collections.emptySet()));
        }

        scheduleScan();
    }

    protected boolean canScanContinueTowards(@Nullable final Direction direction) {
        return true;
    }

    protected boolean canDetectDevicesTowards(@Nullable final Direction direction) {
        return canScanContinueTowards(direction);
    }

    protected Optional<BlockQueryResult> collectDevices(
            final LevelAccessor level, final BlockPos pos, @Nullable final Direction side) {
        final BlockDeviceQuery query =
                Devices.makeQuery(level, pos, side != null ? side.getOpposite() : null);
        final Set<BlockEntry> entries = new HashSet<>();

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

    protected void collectSyntheticDevices(
            final LevelAccessor level,
            final BlockPos pos,
            @Nullable final Direction side,
            final Set<BlockEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        final String blockName = LevelUtils.getBlockName(level, pos);
        if (blockName != null) {
            entries.add(
                    new BlockEntry(
                            new BlockDeviceInfo(null, new TypeNameRPCDevice(blockName)), side));
        }
    }

    @Override
    public void onEntryRemoved(
            final String dataKey, final CompoundTag tag, @Nullable final BlockDeviceQuery query) {
        super.onEntryRemoved(dataKey, tag, query);
        assert query != null : "Passed null query for block device bus element.";
        final Registry<BlockDeviceProvider> registry = Providers.blockDeviceProviderRegistry();
        final BlockDeviceProvider provider = registry.get(ResourceLocation.parse(dataKey));
        if (provider != null) {
            provider.unmount(query, tag);
        }
    }
}