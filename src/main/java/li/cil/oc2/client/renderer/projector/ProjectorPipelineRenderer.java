package li.cil.oc2.client.renderer.projector;

//? if >=26.1 && <26.2 {
/*
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.stage.RenderInfo;
import li.cil.oc2.client.renderer.stage.shader.ModShaders;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

// GPU depth and compositing passes for the 26.1 level frame graph.
public final class ProjectorPipelineRenderer {
    private static final int UNIFORM_SIZE = 128;
    private static final float FRUSTUM_SCALE =
            (ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE + 4f)
                    * ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE;
    private static final float FRUSTUM_X = ((ProjectorBlockEntity.MAX_WIDTH - 1) / 2) / FRUSTUM_SCALE;
    private static final float FRUSTUM_Y = (ProjectorBlockEntity.MAX_HEIGHT - 1) / FRUSTUM_SCALE;
    private static final Matrix4f PROJECTION = new Matrix4f().frustum(
            -FRUSTUM_X, FRUSTUM_X, 0, FRUSTUM_Y, 1f / 16f,
            ProjectorBlockEntity.MAX_RENDER_DISTANCE);
    private static final RenderPipeline DEPTH_SOLID = depthPipeline(false);
    private static final RenderPipeline DEPTH_CUTOUT = depthPipeline(true);
    private static final RenderPipeline COMPOSITE = RenderPipeline.builder()
            .withLocation(id("pipeline/projector_composite"))
            .withVertexShader(id("core/projector_composite"))
            .withFragmentShader(id("core/projector_composite"))
            .withSampler("MainDepth").withSampler("ProjectorDepth").withSampler("ProjectorColor")
            .withUniform("ProjectorMatrices", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(Optional.empty()).withCull(false).build();
    private static final Cache<ProjectorBlockEntity, RenderInfo> FRAMES = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(5))
            .removalListener(ProjectorPipelineRenderer::releaseFrame).build();
    private static RenderTarget depthTarget;
    private static Matrix4fc mainProjection;

    public static void setMainProjection(final Matrix4fc projection) {
        mainProjection = projection;
    }

    private ProjectorPipelineRenderer() {}

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(API.MOD_ID, path);
    }

    private static RenderPipeline depthPipeline(final boolean cutout) {
        final var builder = RenderPipeline.builder()
                .withLocation(id("pipeline/projector_depth_" + (cutout ? "cutout" : "solid")))
                .withVertexShader(id("core/projector_depth"))
                .withFragmentShader(id("core/projector_depth"))
                .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                .withUniform("ChunkSection", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
                .withCull(true);
        if (cutout) builder.withShaderDefine("CUTOUT").withSampler("Sampler0");
        return builder.build();
    }

    private static void releaseFrame(final RemovalNotification<ProjectorBlockEntity, RenderInfo> event) {
        if (event.getKey() != null) event.getKey().setFrameConsumer(null);
        if (event.getValue() != null) event.getValue().close();
    }

    public static void cleanUp() {
        FRAMES.cleanUp();
        if (Minecraft.getInstance().level == null) {
            FRAMES.invalidateAll();
            if (depthTarget != null) {
                depthTarget.destroyBuffers();
                depthTarget = null;
            }
        }
    }

    private static RenderInfo frame(final ProjectorBlockEntity projector) {
        try {
            return FRAMES.get(projector, () -> {
                final RenderInfo info = new RenderInfo(ProjectorDevice.WIDTH, ProjectorDevice.HEIGHT);
                projector.setFrameConsumer(info::processFrame);
                return info;
            });
        } catch (final ExecutionException error) {
            throw new RuntimeException(error);
        }
    }

    public static void render(
            final LevelRenderer renderer,
            final CameraRenderState camera,
            final Matrix4fc mainView,
            final SectionRenderDispatcher.RenderSection[] sections) {
        if (ProjectorDepthRenderer.VISIBLE_PROJECTORS.isEmpty()) return;
        final Minecraft minecraft = Minecraft.getInstance();
        final RenderTarget main = minecraft.getMainRenderTarget();
        final var visible = renderer.getVisibleSections();
        final List<SectionRenderDispatcher.RenderSection> savedSections = new ArrayList<>(visible);
        final List<ProjectorDepthRenderer.VisibleProjector> projectors =
                new ArrayList<>(ProjectorDepthRenderer.VISIBLE_PROJECTORS);
        ProjectorDepthRenderer.VISIBLE_PROJECTORS.clear();
        projectors.sort(Comparator.comparingDouble(entry -> entry.worldPos().distanceToSqr(camera.pos)));
        if (depthTarget == null) depthTarget = new TextureTarget("oc2r projector depth", 512, 512, true);
        final Matrix4f inverseMain = new Matrix4f(mainProjection != null ? mainProjection : camera.projectionMatrix).mul(mainView).invert();
        try {
            for (int index = 0; index < Math.min(projectors.size(), ModShaders.MAX_PROJECTORS); index++) {
                final var entry = projectors.get(index);
                if (entry.projector().isRemoved() || !entry.projector().hasEnergy()) continue;
                final Matrix4f projectorView = new Matrix4f()
                        .rotationY((float) Math.toRadians(entry.yRot() + 180))
                        .translate((float) (camera.pos.x - entry.worldPos().x),
                                (float) (camera.pos.y - entry.worldPos().y),
                                (float) (camera.pos.z - entry.worldPos().z));
                final Matrix4f projectorCamera = new Matrix4f(PROJECTION).mul(projectorView);
                final Frustum frustum = new Frustum(
                        new Matrix4f().rotationY((float) Math.toRadians(entry.yRot() + 180)), PROJECTION);
                frustum.prepare(entry.worldPos().x, entry.worldPos().y, entry.worldPos().z);
                visible.clear();
                for (final var section : sections) {
                    if (section != null && frustum.isVisible(section.getBoundingBox())) visible.add(section);
                }
                final ChunkSectionsToRender chunks = renderer.prepareChunkRenders(projectorView);
                renderDepth(chunks);
                final RenderInfo frame = frame(entry.projector());
                entry.projector().onRendering();
                composite(main, inverseMain, projectorCamera, frame);
            }
        } finally {
            visible.clear();
            visible.addAll(savedSections);
        }
    }

    private static void renderDepth(final ChunkSectionsToRender chunks) {
        final var device = RenderSystem.getDevice();
        device.createCommandEncoder().clearDepthTexture(depthTarget.getDepthTexture(), 1.0);
        try (MemoryStack stack = MemoryStack.stackPush();
                GpuBuffer projection = device.createBuffer(() -> "oc2r projector projection",
                        GpuBuffer.USAGE_UNIFORM, Std140Builder.onStack(stack, 64).putMat4f(PROJECTION).get());
                RenderPass pass = device.createCommandEncoder().createRenderPass(
                        () -> "oc2r projector terrain depth", depthTarget.getColorTextureView(),
                        OptionalInt.empty(), depthTarget.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setUniform("Globals", RenderSystem.getGlobalSettingsUniform());
            pass.setUniform("Projection", projection);
            final var indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            final GpuBuffer indexBuffer = chunks.maxIndicesRequired() == 0 ? null : indices.getBuffer(chunks.maxIndicesRequired());
            for (final ChunkSectionLayer layer : new ChunkSectionLayer[] {ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT}) {
                pass.setPipeline(layer == ChunkSectionLayer.SOLID ? DEPTH_SOLID : DEPTH_CUTOUT);
                if (layer == ChunkSectionLayer.CUTOUT) {
                    pass.bindTexture("Sampler0", chunks.textureView(),
                            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                }
                for (final var draws : chunks.drawGroupsPerLayer().get(layer).values()) {
                    if (!draws.isEmpty()) pass.drawMultipleIndexed(draws, indexBuffer, indices.type(),
                            List.of("ChunkSection"), chunks.chunkSectionInfos());
                }
            }
        }
    }

    private static void composite(final RenderTarget main, final Matrix4f inverseMain,
            final Matrix4f projectorCamera, final RenderInfo frame) {
        final var device = RenderSystem.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush();
                GpuBuffer matrices = device.createBuffer(() -> "oc2r projector matrices", GpuBuffer.USAGE_UNIFORM,
                        Std140Builder.onStack(stack, UNIFORM_SIZE).putMat4f(inverseMain).putMat4f(projectorCamera).get());
                RenderPass pass = device.createCommandEncoder().createRenderPass(
                        () -> "oc2r projector composite", main.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(COMPOSITE);
            pass.setUniform("ProjectorMatrices", matrices);
            final var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            pass.bindTexture("MainDepth", main.getDepthTextureView(), sampler);
            pass.bindTexture("ProjectorDepth", depthTarget.getDepthTextureView(), sampler);
            pass.bindTexture("ProjectorColor", frame.getTexture().getTextureView(), sampler);
            pass.draw(0, 3);
        }
    }
}
*///?}
