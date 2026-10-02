package li.cil.oc2.common.blockentity.network.cable;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import javax.annotation.Nullable;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.info.BlockDeviceInfo;
import li.cil.oc2.common.bus.element.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.bus.element.group.query.BlockEntry;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.common.util.world.level.LevelUtils;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public final class BusCableBusElement extends AbstractBlockDeviceBusElement {
    private final BusCableBlockEntity owner;

    BusCableBusElement(final BusCableBlockEntity owner) {
        super();
        this.owner = owner;
    }

    @Nullable
    @Override
    public Level getLevel() {
        return owner.getLevel();
    }

    @Override
    public BlockPos getPosition() {
        return owner.getBlockPos();
    }

    // FORCED_OFF is only ever asked of the side a scan wants to leave through, so it blocks this
    // cable from reaching out while the cable on the other side can still reach in: it asks about
    // its own face, not ours. The resulting topology is asymmetric (docs/CABLE-SYSTEM.md).
    @Override
    public boolean canScanContinueTowards(@Nullable final Direction direction) {
        if (owner.getFaceOverride(direction).blocksConnection()) {
            return false;
        }
        final ConnectionType connectionType =
                BusCableStateProperties.getConnectionType(owner.getBlockState(), direction);
        return connectionType == ConnectionType.CABLE || connectionType == ConnectionType.INTERFACE;
    }

    @Override
    public boolean canDetectDevicesTowards(@Nullable final Direction direction) {
        if (owner.getFaceOverride(direction).blocksConnection()) {
            return false;
        }
        final ConnectionType connectionType =
                BusCableStateProperties.getConnectionType(owner.getBlockState(), direction);
        return connectionType == ConnectionType.INTERFACE;
    }

    @Override
    protected void collectSyntheticDevices(
            final LevelAccessor level,
            final BlockPos pos,
            @Nullable final Direction side,
            final Set<BlockEntry> entries) {
        super.collectSyntheticDevices(level, pos, side, entries);

        if (side == null || entries.isEmpty()) {
            return;
        }

        final String interfaceName = owner.getInterfaceName(side);
        if (!StringUtil.isNullOrEmpty(interfaceName)) {
            entries.add(
                    new BlockEntry(
                            new BlockDeviceInfo(null, new TypeNameRPCDevice(interfaceName)), side));
        }
    }

    @Override
    public double getEnergyConsumption() {
        return super.getEnergyConsumption()
                + Config.busCableEnergyPerTick
                + BusCableStateProperties.getInterfaceCount(owner.getBlockState())
                        * Config.busInterfaceEnergyPerTick;
    }

    /**
     * Refreshes the devices behind every neighbour once this cable has (re)loaded. A plain
     * topology rescan is not enough: neighbours that finished loading before us never saw a
     * capability listener of ours, so the devices they already published stay invisible until
     * something forces them to publish again.
     */
    void scheduleLateLoad() {
        final Level level = owner.getLevel();
        assert level != null;
        ServerScheduler.schedule(
                level,
                () -> {
                    if (!owner.isValid()) return;
                    final var world = requireNonNull(owner.getLevel());
                    final var pos = owner.getBlockPos();
                    for (final var direction : Constants.DIRECTIONS) {
                        updateDevicesForNeighbor(direction);
                        final var neighborPos = pos.relative(direction);
                        final var blockEntity =
                                LevelUtils.getBlockEntityIfChunkExists(world, neighborPos);
                        if (blockEntity == null) continue;
                        final var capability =
                                Platform.capabilities()
                                        .getBlockCapability(
                                                Capabilities.DeviceBusElement.BLOCK,
                                                world,
                                                neighborPos,
                                                null,
                                                blockEntity,
                                                direction.getOpposite());
                        if (capability != null) capability.scheduleScan();
                    }
                });
    }
}