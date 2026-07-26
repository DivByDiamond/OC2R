package li.cil.oc2.common.blockentity.network;

import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.NetworkConnectorConnectionsMessage;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.ServerScheduler;
import li.cil.oc2.common.util.TickUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

final class NetworkConnectorConnectionManager {
    private static final int RETRY_UNLOADED_CHUNK_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(5));
    private static final int MAX_CONNECTION_COUNT = 2;
    private static final int MAX_CONNECTION_DISTANCE = 16;

    private final NetworkConnectorBlockEntity owner;

    final HashSet<BlockPos> connectorPositions = new HashSet<>();
    final HashSet<BlockPos> ownedCables = new HashSet<>();
    final HashSet<BlockPos> dirtyConnectors = new HashSet<>();
    final HashMap<BlockPos, NetworkConnectorBlockEntity> connectors = new HashMap<>();

    NetworkConnectorConnectionManager(final NetworkConnectorBlockEntity owner) {
        this.owner = owner;
    }

    static ConnectionResult connect(final NetworkConnectorBlockEntity connectorA, final NetworkConnectorBlockEntity connectorB) {
        if (connectorA == connectorB || !connectorA.isValid() || !connectorB.isValid()) {
            return ConnectionResult.FAILURE;
        }

        final Level level = connectorA.getLevel();
        if (level == null || level.isClientSide()) {
            return ConnectionResult.FAILURE;
        }

        if (connectorB.getLevel() != level) {
            return ConnectionResult.FAILURE;
        }

        if (!connectorA.connectionManager.canConnectMore() || !connectorB.connectionManager.canConnectMore()) {
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
        if (connectorA.connectionManager.ownedCables.contains(posB) || connectorB.connectionManager.ownedCables.contains(posA)) {
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

    void disconnectFrom(final BlockPos pos) {
        dirtyConnectors.remove(pos);
        connectors.remove(pos);

        if (ownedCables.remove(pos)) {
            final Level level = owner.getLevel();
            if (level != null) {
                final Vec3 middle = Vec3.atCenterOf(owner.getBlockPos().offset(pos)).scale(0.5f);
                ItemStackUtils.spawnAsEntity(level, middle, new ItemStack(Items.NETWORK_CABLE.get()));
            }
        }

        if (owner.isValid()) {
            if (connectorPositions.remove(pos)) {
                onConnectedPositionsChanged();
            }

            owner.setChanged();
        }
    }

    boolean canConnectMore() {
        return connectorPositions.size() < MAX_CONNECTION_COUNT;
    }

    Collection<BlockPos> getConnectedPositions() {
        return connectorPositions;
    }

    @OnlyIn(Dist.CLIENT)
    void setConnectedPositionsClient(final ArrayList<BlockPos> positions) {
        connectorPositions.clear();
        connectorPositions.addAll(positions);
        NetworkCableRenderer.invalidateConnections();
    }

    void resolveConnectedInterface(final BlockPos connectedPosition) {
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
            ServerScheduler.schedule(level, () -> dirtyConnectors.add(connectedPosition), RETRY_UNLOADED_CHUNK_INTERVAL);
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

        if (NetworkConnectorConnectionValidator.isObstructed(level, owner.getBlockPos(), connectedPosition)) {
            disconnectFrom(connectedPosition);
            networkConnector.connectionManager.disconnectFrom(owner.getBlockPos());
            return;
        }

        connectors.put(connectedPosition, networkConnector);
    }

    private void onConnectedPositionsChanged() {
        final Level level = owner.getLevel();
        if (level != null && !level.isClientSide()) {
            final NetworkConnectorConnectionsMessage message = new NetworkConnectorConnectionsMessage(owner);
            Network.sendToClientsTrackingBlockEntity(message, owner);
        }
    }

}
