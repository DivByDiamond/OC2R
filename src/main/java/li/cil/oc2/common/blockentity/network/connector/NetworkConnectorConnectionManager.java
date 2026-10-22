package li.cil.oc2.common.blockentity.network.connector;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.common.util.tick.TickUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class NetworkConnectorConnectionManager {
    private static final int RETRY_UNLOADED_CHUNK_INTERVAL =
            TickUtils.toTicks(Duration.ofSeconds(5));

    private final NetworkConnectorBlockEntity owner;

    public final Set<BlockPos> connectorPositions = new HashSet<>();
    public final Set<BlockPos> ownedCables = new HashSet<>();
    public final Set<BlockPos> dirtyConnectors = new HashSet<>();
    public final Map<BlockPos, NetworkConnectorBlockEntity> connectors = new ConcurrentHashMap<>();

    NetworkConnectorConnectionManager(final NetworkConnectorBlockEntity owner) {
        this.owner = owner;
    }

    public static ConnectionResult connect(
            final NetworkConnectorBlockEntity connectorA,
            final NetworkConnectorBlockEntity connectorB) {
        final ConnectionResult validation =
                NetworkConnectorConnectionValidator.validate(connectorA, connectorB);
        if (validation != null) {
            return validation;
        }

        return establishConnection(
                connectorA, connectorB, connectorA.getBlockPos(), connectorB.getBlockPos());
    }

    private static ConnectionResult establishConnection(
            final NetworkConnectorBlockEntity connectorA,
            final NetworkConnectorBlockEntity connectorB,
            final BlockPos posA,
            final BlockPos posB) {
        if (connectorA.connectionManager.connectorPositions.add(posB)) {
            connectorA.connectionManager.dirtyConnectors.add(posB);
            connectorA.connectionManager.onConnectedPositionsChanged();
        }

        if (connectorB.connectionManager.connectorPositions.add(posA)) {
            connectorB.connectionManager.dirtyConnectors.add(posA);
            connectorB.connectionManager.onConnectedPositionsChanged();
        }

        final ConnectionResult result;
        if (connectorA.connectionManager.ownedCables.contains(posB)
                || connectorB.connectionManager.ownedCables.contains(posA)) {
            connectorA.connectionManager.ownedCables.add(posB);
            connectorB.connectionManager.ownedCables.remove(posA);
            result = ConnectionResult.ALREADY_CONNECTED;
        } else {
            connectorA.connectionManager.ownedCables.add(posB);
            result = ConnectionResult.SUCCESS;
        }

        connectorA.setChanged();
        connectorB.setChanged();

        return result;
    }

    public void disconnectFrom(final BlockPos pos) {
        disconnectFrom(pos, true);
    }

    /**
     * Removes the link to {@code pos} from this connector.
     *
     * @param dropCable whether the cable item this connector owns should be spawned back into the
     *                  world. {@code false} for removals started by a creative player, who never
     *                  paid a cable for the link and must not gain one from removing it.
     */
    public void disconnectFrom(final BlockPos pos, final boolean dropCable) {
        dirtyConnectors.remove(pos);
        connectors.remove(pos);

        final boolean owned = ownedCables.remove(pos);
        // The validator rejects self-links at connect() time, so this can only be reached through a
        // corrupted or hand-edited save; guard it anyway rather than spawning the cable on top of
        // the connector itself.
        if (owned && dropCable && !pos.equals(owner.getBlockPos())) {
            final Level level = owner.getLevel();
            if (level != null) {
                // Halfway between both connectors, so the cable lands inside the structure.
                final Vec3 middle = Vec3.atCenterOf(owner.getBlockPos())
                        .add(Vec3.atCenterOf(pos))
                        .scale(0.5f);
                ItemStackUtils.spawnAsEntity(
                        level, middle, new ItemStack(Items.NETWORK_CABLE.get()));
            }
        }

        if (owner.isValid()) {
            if (connectorPositions.remove(pos)) {
                onConnectedPositionsChanged();
            }

            owner.setChanged();
        }
    }

    public boolean canConnectMore() {
        return connectorPositions.size() < Config.networkConnectorPorts;
    }

    public Collection<BlockPos> getConnectedPositions() {
        return connectorPositions;
    }

    public void resolveConnectedInterface(final BlockPos connectedPosition) {
        connectors.remove(connectedPosition);

        if (!owner.isValid()) {
            return;
        }

        final Level level = owner.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        //? if >=26.1 {
        /*final ChunkPos destinationChunk = ChunkPos.containing(connectedPosition);
        if (!level.hasChunk(destinationChunk.x(), destinationChunk.z())) {
        *///?} else {
        final ChunkPos destinationChunk = new ChunkPos(connectedPosition);
        if (!level.hasChunk(destinationChunk.x, destinationChunk.z)) {
        //?}
            ServerScheduler.schedule(
                    level,
                    () -> dirtyConnectors.add(connectedPosition),
                    RETRY_UNLOADED_CHUNK_INTERVAL);
            return;
        }

        final BlockEntity blockEntity = level.getBlockEntity(connectedPosition);
        if (!(blockEntity instanceof final NetworkConnectorBlockEntity networkConnector)) {
            disconnectFrom(connectedPosition);
            return;
        }

        if (!networkConnector.connectionManager.connectorPositions.contains(owner.getBlockPos())) {
            // The other end does not know about this link (for example it was removed while this
            // side was unloaded): drop our half instead of keeping a phantom cable.
            disconnectFrom(connectedPosition);
            return;
        }

        if (!connectedPosition.closerThan(owner.getBlockPos(),
                NetworkConnectorConnectionValidator.MAX_CONNECTION_DISTANCE)) {
            disconnectFrom(connectedPosition);
            networkConnector.connectionManager.disconnectFrom(owner.getBlockPos());
            return;
        }

        if (NetworkConnectorConnectionValidator.isObstructed(
                level, owner.getBlockPos(), connectedPosition)) {
            disconnectFrom(connectedPosition);
            networkConnector.connectionManager.disconnectFrom(owner.getBlockPos());
            return;
        }

        connectors.put(connectedPosition, networkConnector);
    }

    private void onConnectedPositionsChanged() {
        final Level level = owner.getLevel();
        if (level != null && !level.isClientSide()) {
            // The implicit BlockEntityDataPacket triggered here carries getUpdateTag,
            // which already includes the connection positions; a separate custom
            // message would duplicate that payload for every tracker.
            level.sendBlockUpdated(
                    owner.getBlockPos(),
                    owner.getBlockState(),
                    owner.getBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }
}