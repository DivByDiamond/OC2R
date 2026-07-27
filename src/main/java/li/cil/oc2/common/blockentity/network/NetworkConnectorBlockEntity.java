package li.cil.oc2.common.blockentity.network;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.network.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.util.tick.TickUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
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

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final NetworkConnectorBlockEntity self) {
                        if (side
                                == NetworkConnectorBlock.getFacing(self.getBlockState())
                                        .getOpposite()) return self.networkInterface;
                    }
                    return null;
                },
                Blocks.NETWORK_CONNECTOR.get());
    }

    @Override
    protected void loadClient() {
        super.loadClient();
        NetworkCableRenderer.addNetworkConnector(this);
    }

    @Override
    protected void loadServer() {
        super.loadServer();

        final var level = (ServerLevel) this.level;
        final Direction facing = NetworkConnectorBlock.getFacing(getBlockState());
        final BlockPos sourcePos = getBlockPos().relative(facing.getOpposite());
        level.registerCapabilityListener(sourcePos, this.adjacentInterfaceListener);
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);

        if (isRemove) {
            final List<NetworkConnectorBlockEntity> list =
                    new ArrayList<>(connectionManager.connectors.values());
            connectionManager.connectors.clear();
            for (final NetworkConnectorBlockEntity connector : list) {
                connectionManager.disconnectFrom(connector.getBlockPos());
                connector.connectionManager.disconnectFrom(getBlockPos());
            }
        } else {
            final BlockPos pos = getBlockPos();
            for (final NetworkConnectorBlockEntity connector :
                    connectionManager.connectors.values()) {
                connector.connectionManager.connectors.remove(pos);
                if (connector.connectionManager.connectorPositions.contains(pos)) {
                    connector.connectionManager.dirtyConnectors.add(pos);
                }
            }
        }
    }

    private void resolveLocalInterface() {
        assert level != null;

        if (!isValid()) {
            adjacentInterface = null;
            return;
        }

        final Direction facing = NetworkConnectorBlock.getFacing(getBlockState());
        final BlockPos sourcePos = getBlockPos().relative(facing.getOpposite());

        if (!level.isLoaded(sourcePos)) {
            adjacentInterface = null;
            return;
        }

        adjacentInterface =
                level.getCapability(Capabilities.NetworkInterface.BLOCK, sourcePos, facing);
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