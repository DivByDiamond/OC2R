package li.cil.oc2.client.renderer.cable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.Collections;
import li.cil.oc2.common.blockentity.network.connector.NetworkConnectorBlockEntity;
import li.cil.oc2.platform.event.ClientEvents;
import li.cil.oc2.platform.event.CommonEvents;
import li.cil.oc2.platform.event.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

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

    public static void registerEvents() {
        CommonEvents.CHUNK_UNLOAD.register(NetworkCableRenderer::handleChunkUnload);
        CommonEvents.LEVEL_UNLOAD.register(NetworkCableRenderer::handleLevelUnload);
        ClientEvents.RENDER_LEVEL.register(NetworkCableRenderer::handleRenderWorld);
    }

    private static void handleChunkUnload(final LevelAccessor level, final ChunkAccess chunk) {
        if (level.isClientSide()) {
            final ChunkPos chunkPos = chunk.getPos();

            final List<NetworkConnectorBlockEntity> list = new ArrayList<>(connectors);
            for (final NetworkConnectorBlockEntity connector : list) {
                final ChunkPos connectorChunkPos =
                        new ChunkPos(connector.getBlockPos()); // NOPMD: depends on connector
                if (Objects.equals(connectorChunkPos, chunkPos)) {
                    connectors.remove(connector);
                }
            }

            invalidateConnections();
        }
    }

    private static void handleLevelUnload(final LevelAccessor level) {
        if (level.isClientSide()) {

            final List<NetworkConnectorBlockEntity> list = new ArrayList<>(connectors);
            for (final NetworkConnectorBlockEntity connector : list) {
                if (connector.getLevel().equals(level)) {
                    connectors.remove(connector);
                }
            }

            invalidateConnections();
        }
    }

    private static void handleRenderWorld(final LevelRenderContext event) {
        if (event.stage() != LevelRenderContext.Stage.AFTER_CUTOUT_BLOCKS) {
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

        final PoseStack stack = event.poseStack();

        final Vec3 eye = event.camera().getPosition();

        final var frustumMatrix = new Matrix4f(event.modelViewMatrix());
        frustumMatrix.mul(stack.last().pose());
        final Frustum frustum = new Frustum(frustumMatrix, event.projectionMatrix());
        frustum.prepare(eye.x, eye.y, eye.z);

        stack.pushPose();
        stack.translate(-eye.x, -eye.y, -eye.z);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().set(event.modelViewMatrix());
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
                        new NetworkCableConnection(// NOPMD: depends on loop positions
                                position, connectedPosition);
                if (seen.add(connection)) {
                    connections.add(connection);
                    connectionsByConnector
                            .computeIfAbsent(// NOPMD: per-connector list
                                    connector, unused -> new ArrayList<>()) // NOPMD allocation depends on loop iteration / per-item state
                            .add(connection);
                }
            }
        }
    }
}