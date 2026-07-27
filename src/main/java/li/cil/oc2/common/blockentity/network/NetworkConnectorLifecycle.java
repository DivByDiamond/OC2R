package li.cil.oc2.common.blockentity.network;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.block.network.NetworkConnectorBlock;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = API.MOD_ID)
final class NetworkConnectorLifecycle {

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
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

    static void loadClient(final NetworkConnectorBlockEntity entity) {
        NetworkCableRenderer.addNetworkConnector(entity);
    }

    static void loadServer(final NetworkConnectorBlockEntity entity) {
        final var level = (ServerLevel) entity.level;
        final Direction facing = NetworkConnectorBlock.getFacing(entity.getBlockState());
        final BlockPos sourcePos = entity.getBlockPos().relative(facing.getOpposite());
        level.registerCapabilityListener(sourcePos, entity.adjacentInterfaceListener);
    }

    static void unloadServer(
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

    static void resolveLocalInterface(final NetworkConnectorBlockEntity entity) {
        assert entity.level != null;

        if (!entity.isValid()) {
            entity.adjacentInterface = null;
            return;
        }

        final Direction facing = NetworkConnectorBlock.getFacing(entity.getBlockState());
        final BlockPos sourcePos = entity.getBlockPos().relative(facing.getOpposite());

        if (!entity.level.isLoaded(sourcePos)) {
            entity.adjacentInterface = null;
            return;
        }

        entity.adjacentInterface =
                entity.level.getCapability(Capabilities.NetworkInterface.BLOCK, sourcePos, facing);
    }
}
