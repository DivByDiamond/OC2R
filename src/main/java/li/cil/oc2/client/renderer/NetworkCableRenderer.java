package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.Collections;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.network.NetworkConnectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(value = Dist.CLIENT, modid = API.MOD_ID)
public final class NetworkCableRenderer {
    private static final Set<NetworkConnectorBlockEntity> connectors =
            Collections.newSetFromMap(Collections.synchronizedMap(Collections.synchronizedMap(Collections.synchronizedMap(new WeakHashMap<>()))));
    private static int lastKnownConnectorCount;
    private static boolean isDirty;

    private static final List<NetworkCableConnection> connections = new ArrayList<>();
    private static final Map<NetworkConnectorBlockEntity, List<NetworkCableConnection>>
            connectionsByConnector = Collections.synchronizedMap(Collections.synchronizedMap(Collections.synchronizedMap(new WeakHashMap<>())));

    public static void addNetworkConnector(final NetworkConnectorBlockEntity connector) {
        connectors.add(connector);
        invalidateConnections();
    }

    public static void invalidateConnections() {
        isDirty = true;
    }

    @SubscribeEvent
    public static void handleChunkUnloadEvent(final ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            final ChunkPos chunkPos = event.getChunk().getPos();

            final List<NetworkConnectorBlockEntity> list = new ArrayList<>(connectors);
            for (final NetworkConnectorBlockEntity connector : list) {
                final ChunkPos connectorChunkPos = new ChunkPos(connector.getBlockPos());
                if (Objects.equals(connectorChunkPos, chunkPos)) {
                    connectors.remove(connector);
                }
            }

            invalidateConnections();
        }
    }

    @SubscribeEvent
    public static void handleWorldUnloadEvent(final LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            final LevelAccessor level = event.getLevel();

            final List<NetworkConnectorBlockEntity> list = new ArrayList<>(connectors);
            for (final NetworkConnectorBlockEntity connector : list) {
                if (connector.getLevel().equals(level)) {
                    connectors.remove(connector);
                }
            }

            invalidateConnections();
        }
    }

    @SubscribeEvent
    public static void handleRenderWorld(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return;
        }

        validateConnectors();
        validatePairs();

        if (connections.isEmpty()) {
            return;
        }

        final Minecraft client = Minecraft.getInstance();
        final Level level = client.level;
        if (level == null) {
            return;
        }

        final PoseStack stack = event.getPoseStack();

        final Vec3 eye = event.getCamera().getPosition();

        final var frustumMatrix = new Matrix4f(event.getModelViewMatrix());
        frustumMatrix.mul(stack.last().pose());
        final Frustum frustum = new Frustum(frustumMatrix, event.getProjectionMatrix());
        frustum.prepare(eye.x, eye.y, eye.z);

        stack.pushPose();
        stack.translate(-eye.x, -eye.y, -eye.z);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();

        CableRenderUtils.renderCables(level, stack, eye, connections, frustum::isVisible);

        RenderSystem.getModelViewStack().popMatrix();

        stack.popPose();
    }

    private static void validateConnectors() {
        final List<NetworkConnectorBlockEntity> list = new ArrayList<>(connectors);
        for (final NetworkConnectorBlockEntity connector : list) {
            if (!connector.isValid()) {
                connectors.remove(connector);
                connectionsByConnector.remove(connector);
                invalidateConnections();
            }
        }

        if (list.size() != lastKnownConnectorCount) {
            invalidateConnections();
        }
        lastKnownConnectorCount = list.size();
    }

    private static void validatePairs() {
        if (!isDirty) {
            return;
        }

        isDirty = false;
        connections.clear();
        connectionsByConnector.clear();

        final Set<NetworkCableConnection> seen = new HashSet<>();
        for (final NetworkConnectorBlockEntity connector : connectors) {
            final BlockPos position = connector.getBlockPos();
            for (final BlockPos connectedPosition : connector.getConnectedPositions()) {
                final NetworkCableConnection connection =
                        new NetworkCableConnection(position, connectedPosition);
                if (seen.add(connection)) {
                    connections.add(connection);
                    connectionsByConnector
                            .computeIfAbsent(connector, unused -> new ArrayList<>())
                            .add(connection);
                }
            }
        }
    }
}