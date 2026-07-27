package li.cil.oc2.common.blockentity.network;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.util.tick.TickUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;

public final class NetworkConnectorBlockEntity extends ModBlockEntity
        implements TickableBlockEntity {
    private static final int BYTES_PER_TICK = 64 * 1024 / TickUtils.toTicks(Duration.ofSeconds(1));
    private static final int MIN_ETHERNET_FRAME_SIZE = 42;

    final NetworkConnectorInterface networkInterface = new NetworkConnectorInterface(this);
    final NetworkConnectorConnectionManager connectionManager =
            new NetworkConnectorConnectionManager(this);

    @SuppressWarnings("FieldCanBeLocal")
    private final ICapabilityInvalidationListener adjacentInterfaceListener =
            () -> {
                this.isAdjacentInterfaceDirty = true;
                return true;
            };

    private boolean isAdjacentInterfaceDirty = true;
    @Nullable NetworkInterface adjacentInterface = null;

    public NetworkConnectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.NETWORK_CONNECTOR.get(), pos, state);
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }

        if (isAdjacentInterfaceDirty) {
            isAdjacentInterfaceDirty = false;
            resolveLocalInterface();
        }

        if (!connectionManager.dirtyConnectors.isEmpty()) {
            final List<BlockPos> list = new ArrayList<>(connectionManager.dirtyConnectors);
            connectionManager.dirtyConnectors.clear();
            for (final BlockPos connectedPosition : list) {
                connectionManager.resolveConnectedInterface(connectedPosition);
            }
        }

        NetworkInterface source = adjacentInterface;
        if (source == null) source = NullNetworkInterface.INSTANCE;

        int byteBudget = BYTES_PER_TICK;
        byte[] frame = source.readEthernetFrame();
        while (frame != null && byteBudget > 0) {
            byteBudget -= Math.max(frame.length, MIN_ETHERNET_FRAME_SIZE);
            networkInterface.writeEthernetFrame(source, frame, Config.ethernetFrameTimeToLive);
            frame = source.readEthernetFrame();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        NetworkConnectorConnectionStore.writeToUpdateTag(
                tag, registries, connectionManager.connectorPositions);
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        NetworkConnectorConnectionStore.readFromUpdateTag(
                tag,
                registries,
                connectionManager.connectorPositions,
                connectionManager.dirtyConnectors);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NetworkConnectorConnectionStore.save(
                tag,
                registries,
                connectionManager.connectorPositions,
                connectionManager.ownedCables);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NetworkConnectorConnectionStore.load(
                tag,
                registries,
                connectionManager.connectorPositions,
                connectionManager.dirtyConnectors,
                connectionManager.ownedCables);
    }

    @Override
    protected void loadClient() {
        super.loadClient();
        NetworkConnectorLifecycle.loadClient(this);
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        NetworkConnectorLifecycle.loadServer(this);
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);
        NetworkConnectorLifecycle.unloadServer(this, isRemove);
    }

    private void resolveLocalInterface() {
        NetworkConnectorLifecycle.resolveLocalInterface(this);
    }

    public static ConnectionResult connect(
            final NetworkConnectorBlockEntity connectorA,
            final NetworkConnectorBlockEntity connectorB) {
        return NetworkConnectorConnectionManager.connect(connectorA, connectorB);
    }

    public void disconnectFrom(final BlockPos pos) {
        connectionManager.disconnectFrom(pos);
    }

    public boolean canConnectMore() {
        return connectionManager.canConnectMore();
    }

    public Collection<BlockPos> getConnectedPositions() {
        return connectionManager.getConnectedPositions();
    }

    @OnlyIn(Dist.CLIENT)
    public void setConnectedPositionsClient(final List<BlockPos> positions) {
        connectionManager.setConnectedPositionsClient(positions);
    }
}