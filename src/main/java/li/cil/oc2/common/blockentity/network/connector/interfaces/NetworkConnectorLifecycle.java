package li.cil.oc2.common.blockentity.network.connector.interfaces;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.network.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.CapabilityRegistrar;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

public final class NetworkConnectorLifecycle {

    public static void registerCapabilities(final CapabilityRegistrar registrar) {
        registrar.registerBlock(
                Capabilities.NetworkInterface.BLOCK,
                (level, pos, state, be, side) -> {
                    if (be instanceof final NetworkConnectorBlockEntity self
                            && side
                                    == NetworkConnectorBlock.getFacing(self.getBlockState())
                                            .getOpposite()) {
                        return self.networkInterface;
                    }
                    return null;
                },
                Blocks.NETWORK_CONNECTOR.get());
    }

    public static void loadClient(final NetworkConnectorBlockEntity entity) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            li.cil.oc2.client.hooks.NetworkCableRendererHooks.addNetworkConnector(entity);
        }
    }

    public static void loadServer(final NetworkConnectorBlockEntity entity) {
        final var level = (ServerLevel) entity.getLevel();
        final Direction facing = NetworkConnectorBlock.getFacing(entity.getBlockState());
        final BlockPos sourcePos = entity.getBlockPos().relative(facing.getOpposite());
        Platform.capabilities()
                .registerBlockCapabilityListener(
                        level, sourcePos, entity.adjacentInterfaceListener);
    }

    public static void unloadServer(
            final NetworkConnectorBlockEntity entity, final boolean isRemove) {
        if (isRemove) {
            final List<NetworkConnectorBlockEntity> list =
                    new ArrayList<>(entity.connectionManager.connectors.values());
            entity.connectionManager.connectors.clear();
            for (final NetworkConnectorBlockEntity connector : list) {
                entity.connectionManager.disconnectFrom(connector.getBlockPos());
                connector.connectionManager.disconnectFrom(entity.getBlockPos());
            }
        } else {
            final BlockPos pos = entity.getBlockPos();
            for (final NetworkConnectorBlockEntity connector :
                    entity.connectionManager.connectors.values()) {
                connector.connectionManager.connectors.remove(pos);
                if (connector.connectionManager.connectorPositions.contains(pos)) {
                    connector.connectionManager.dirtyConnectors.add(pos);
                }
            }
        }
    }

    public static void resolveLocalInterface(final NetworkConnectorBlockEntity entity) {
        assert entity.getLevel() != null;

        if (!entity.isValid()) {
            entity.adjacentInterface = null;
            return;
        }

        final Direction facing = NetworkConnectorBlock.getFacing(entity.getBlockState());
        final BlockPos sourcePos = entity.getBlockPos().relative(facing.getOpposite());

        if (!entity.getLevel().isLoaded(sourcePos)) {
            entity.adjacentInterface = null;
            return;
        }

        entity.adjacentInterface =
                Platform.capabilities()
                        .getBlockCapability(
                                Capabilities.NetworkInterface.BLOCK,
                                entity.getLevel(),
                                sourcePos,
                                facing);
    }
}
