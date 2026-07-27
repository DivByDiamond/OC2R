package li.cil.oc2.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

public class MonitorGUIRenderer {
    private final transient Set<RendererModel> renderers =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    @OnlyIn(Dist.CLIENT)
    public RendererView getRenderer(MonitorBlockEntity monitor) {
        final Renderer renderer = new Renderer(monitor);
        renderers.add(renderer);
        return renderer;
    }

    @OnlyIn(Dist.CLIENT)
    public void releaseRenderer(final MonitorGUIRenderer.RendererView renderer) {
        if (renderer instanceof final MonitorGUIRenderer.RendererModel rendererModel) {
            rendererModel.close();
            renderers.remove(rendererModel);
        }
    }

    private static void handleProjectorNoLongerRendering(
            final RemovalNotification<MonitorBlockEntity, RenderInfo> notification) {
        final MonitorBlockEntity monitor = notification.getKey();
        if (monitor != null) {
            monitor.video.setFrameConsumer(null);
        }
        final RenderInfo renderInfo = notification.getValue();
        if (renderInfo != null) {
            renderInfo.close();
        }
    }

    private interface RendererModel {
        void close();
    }

    public interface RendererView {
        void render(
                final PoseStack stack,
                final Matrix4f projectionMatrix,
                float width,
                float height,
                boolean renderingToBlock);
    }

    @OnlyIn(Dist.CLIENT)
    private record Renderer(MonitorBlockEntity monitorBlock)
            implements RendererModel, RendererView {
        private static final Cache<MonitorBlockEntity, RenderInfo> RENDER_INFO =
                CacheBuilder.newBuilder()
                        .expireAfterAccess(Duration.ofSeconds(5))
                        .removalListener(MonitorGUIRenderer::handleProjectorNoLongerRendering)
                        .build();

        private static DynamicTexture getColorBuffer(final MonitorBlockEntity monitor) {
            try {
                return RENDER_INFO
                        .get(
                                monitor,
                                () -> {
                                    final DynamicTexture texture =
                                            new DynamicTexture(
                                                    MonitorDevice.WIDTH,
                                                    MonitorDevice.HEIGHT,
                                                    false);
                                    texture.upload();
                                    final RenderInfo renderInfo = new RenderInfo(texture);
                                    monitor.video.setFrameConsumer(renderInfo);
                                    return renderInfo;
                                })
                        .texture();
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {}

        @Override
        public void render(
                final PoseStack stack,
                final Matrix4f projectionMatrix,
                float width,
                float height,
                boolean renderingToBlock) {
            if (monitorBlock.isValid()) {
                DynamicTexture texture = getColorBuffer(monitorBlock);
                monitorBlock.video.onRendering();

                RenderSystem.backupProjectionMatrix();
                RenderSystem.getModelViewStack().pushMatrix();

                RenderSystem.enableBlend();
                RenderSystem.blendFunc(
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.depthMask(false);

                final ShaderInstance shader = GameRenderer.getPositionTexShader();

                if (shader == null) return;

                final BufferBuilder builder =
                        Tesselator.getInstance()
                                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

                RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);

                RenderSystem.setShaderTexture(0, texture.getId());

                VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);

                builder.addVertex(0, 0, 0).setUv(0, 0);
                builder.addVertex(0, height, 0).setUv(0, 1);
                builder.addVertex(width, height, 0).setUv(1, 1);
                builder.addVertex(width, 0, 0).setUv(1, 0);

                buffer.bind();
                buffer.upload(builder.buildOrThrow());

                var modelViewMatrix = stack.last().pose();
                if (renderingToBlock) {
                    // Sable/Sodium compatibility: push our pose onto the
                    // RenderSystem model-view stack and apply, so the
                    // cached matrix is current before we read it. Sable
                    // may modify the stack without calling
                    // applyModelViewMatrix(), leaving the cached field
                    // stale — which would cause the monitor image to be
                    // drawn at the wrong position or off-screen.
                    RenderSystem.getModelViewStack().mul(stack.last().pose());
                    RenderSystem.applyModelViewMatrix();
                    modelViewMatrix = RenderSystem.getModelViewMatrix();
                }
                buffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);

                VertexBuffer.unbind();
                buffer.close();

                RenderSystem.restoreProjectionMatrix();
                RenderSystem.getModelViewStack().popMatrix();
                RenderSystem.applyModelViewMatrix();

                RenderSystem.depthMask(true);
            }
        }
    }
}