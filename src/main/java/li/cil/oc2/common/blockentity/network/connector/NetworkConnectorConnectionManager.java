package li.cil.oc2.common.blockentity.network.connector;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.client.renderer.cable.NetworkCableRenderer;
import li.cil.oc2.common.blockentity.network.connector.interfaces.ConnectionResult;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.network.NetworkConnectorConnectionsMessage;
import li.cil.oc2.common.util.item.ItemStackUtils;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.common.util.tick.TickUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public final class NetworkConnectorConnectionManager {
    private static final int RETRY_UNLOADED_CHUNK_INTERVAL =
            TickUtils.toTicks(Duration.ofSeconds(5));
    private static final int MAX_CONNECTION_COUNT = 2;
    private static final int MAX_CONNECTION_DISTANCE = 16;

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
        if (connectorA.equals(connectorB) || !connectorA.isValid() || !connectorB.isValid()) {
            return ConnectionResult.FAILURE;
        }

        final Level level = connectorA.getLevel();
        if (level == null || level.isClientSide()) {
            return ConnectionResult.FAILURE;
        }

        if (!level.equals(connectorB.getLevel())) {
            return ConnectionResult.FAILURE;
        }

        if (!connectorA.connectionManager.canConnectMore()
                || !connectorB.connectionManager.canConnectMore()) {
            return ConnectionResult.FAILURE_FULL;
        }

        final BlockPos posA = connectorA.getBlockPos();
        final BlockPos posB = connectorB.getBlockPos();

        if (!posA.closerThan(posB, MAX_CONNECTION_DISTANCE)) {
            return ConnectionResult.FAILURE_TOO_FAR;
        }

        if (NetworkConnectorConnectionValidator.isObstructed(level, posA, posB)) {
            return ConnectionResult.FAILURE_OBSTRUCTED;
        }

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
        dirtyConnectors.remove(pos);
        connectors.remove(pos);

        if (ownedCables.remove(pos)) {
            final Level level = owner.getLevel();
            if (level != null) {
                final Vec3 middle = Vec3.atCenterOf(owner.getBlockPos().offset(pos)).scale(0.5f);
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
        return connectorPositions.size() < MAX_CONNECTION_COUNT;
    }

    public Collection<BlockPos> getConnectedPositions() {
        return connectorPositions;
    }

    @OnlyIn(Dist.CLIENT)
    public void setConnectedPositionsClient(final List<BlockPos> positions) {
        connectorPositions.clear();
        connectorPositions.addAll(positions);
        NetworkCableRenderer.invalidateConnections();
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

        final ChunkPos destinationChunk = new ChunkPos(connectedPosition);
        if (!level.hasChunk(destinationChunk.x, destinationChunk.z)) {
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

        if (!connectedPosition.closerThan(owner.getBlockPos(), MAX_CONNECTION_DISTANCE)) {
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
            final NetworkConnectorConnectionsMessage message =
                    new NetworkConnectorConnectionsMessage(owner);
            NetworkMessages.sendToClientsTrackingBlockEntity(message, owner);
        }
    }
}