package li.cil.oc2.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import li.cil.oc2.common.blockentity.MonitorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.MonitorDevice;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;

public class MonitorGUIRenderer {
    private final transient Set<RendererModel> renderers = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

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

    private static void handleProjectorNoLongerRendering(final RemovalNotification<MonitorBlockEntity, Renderer.RenderInfo> notification) {
        final MonitorBlockEntity monitor = notification.getKey();
        if (monitor != null) {
            monitor.setFrameConsumer(null);
        }
        final Renderer.RenderInfo renderInfo = notification.getValue();
        if (renderInfo != null) {
            renderInfo.close();
        }
    }

    private interface RendererModel {
        void close();
    }

    public interface RendererView {
        void render(final PoseStack stack, final Matrix4f projectionMatrix, float width, float height, boolean renderingToBlock);
    }

    @OnlyIn(Dist.CLIENT)
        private record Renderer(MonitorBlockEntity monitorBlock) implements RendererModel, RendererView {
            private record RenderInfo(DynamicTexture texture) implements MonitorBlockEntity.FrameConsumer {
                private static final ThreadLocal<byte[]> RGB = ThreadLocal.withInitial(() -> new byte[3]);

                public synchronized void close() {
                    texture.close();
                }

                @Override
                public synchronized void processFrame(final Picture picture) {
                    final NativeImage image = texture.getPixels();
                    if (image == null) {
                        return;
                    }

                    final byte[] y = picture.getPlaneData(0);
                    final byte[] u = picture.getPlaneData(1);
                    final byte[] v = picture.getPlaneData(2);

                    // Convert in quads, based on the half resolution of UV. As such, skip every other row, since
                    // we're setting the current and the next.
                    int lumaIndex = 0, chromaIndex = 0;
                    for (int halfRow = 0; halfRow < MonitorDevice.HEIGHT / 2; halfRow++, lumaIndex += MonitorDevice.WIDTH * 2) {
                        final int row = halfRow * 2;
                        for (int halfCol = 0; halfCol < MonitorDevice.WIDTH / 2; halfCol++, chromaIndex++) {
                            final int col = halfCol * 2;
                            final int yIndex = lumaIndex + col;
                            final byte cb = u[chromaIndex];
                            final byte cr = v[chromaIndex];
                            setFromYUV420(image, col, row, y[yIndex], cb, cr);
                            setFromYUV420(image, col + 1, row, y[yIndex + 1], cb, cr);
                            setFromYUV420(image, col, row + 1, y[yIndex + MonitorDevice.WIDTH], cb, cr);
                            setFromYUV420(image, col + 1, row + 1, y[yIndex + MonitorDevice.WIDTH + 1], cb, cr);
                        }
                    }

                    texture.upload();
                }

                private static void setFromYUV420(final NativeImage image, final int col, final int row, final byte y, final byte cb, final byte cr) {
                    final byte[] bytes = RGB.get();
                    Yuv420jToRgb.YUVJtoRGB(y, cb, cr, bytes, 0);
                    final int r = bytes[0] + 128;
                    final int g = bytes[1] + 128;
                    final int b = bytes[2] + 128;
                    image.setPixelRGBA(col, row, r | (g << 8) | (b << 16) | (0xFF << 24));
                }
            }

        private static final Cache<MonitorBlockEntity, RenderInfo> RENDER_INFO = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(5))
                .removalListener(MonitorGUIRenderer::handleProjectorNoLongerRendering)
                .build();

            private static DynamicTexture getColorBuffer(final MonitorBlockEntity monitor) {
                try {
                    return RENDER_INFO.get(monitor, () -> {
                        final DynamicTexture texture = new DynamicTexture(MonitorDevice.WIDTH, MonitorDevice.HEIGHT, false);
                        texture.upload();
                        final RenderInfo renderInfo = new RenderInfo(texture);
                        monitor.setFrameConsumer(renderInfo);
                        return renderInfo;
                    }).texture();
                } catch (final ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void close() {

            }

            @Override
            public void render(final PoseStack stack, final Matrix4f projectionMatrix, float width, float height, boolean renderingToBlock) {
                if (monitorBlock.isValid()) {
                    DynamicTexture texture = getColorBuffer(monitorBlock);
                    monitorBlock.onRendering();

                    RenderSystem.backupProjectionMatrix();
                    RenderSystem.getModelViewStack().pushMatrix();

                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

                    RenderSystem.colorMask(true, true, true, true);
                    RenderSystem.depthMask(false);

                    final ShaderInstance shader = GameRenderer.getPositionTexShader();

                    if (shader == null) return;

                    final BufferBuilder builder = Tesselator.getInstance()
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
