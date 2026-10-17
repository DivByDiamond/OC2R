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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
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
import li.cil.oc2.client.mixin.ParticleEngineAccessor;
import li.cil.oc2.client.renderer.stage.RenderInfo;
import li.cil.oc2.client.renderer.stage.shader.ModShaders;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    // Entities and particles are not part of the chunk meshes; they cast shadows as boxes.
    private static final RenderPipeline DEPTH_BOXES = RenderPipeline.builder()
            .withLocation(id("pipeline/projector_depth_boxes"))
            .withVertexShader(id("core/projector_depth_box"))
            .withFragmentShader(id("core/projector_depth_box"))
            .withUniform("BoxMatrices", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE))
            .withCull(false).build();
    private static final int MAX_OCCLUDER_BOXES = 2048;
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
                renderDepth(chunks, occluders(frustum, entry.worldPos()), entry.worldPos(), camera.pos, projectorCamera);
                final RenderInfo frame = frame(entry.projector());
                entry.projector().onRendering();
                composite(main, inverseMain, projectorCamera, frame);
            }
        } finally {
            visible.clear();
            visible.addAll(savedSections);
        }
    }

    // Entity hitboxes plus particle billboards as {centerX, centerY, centerZ, halfSize}.
    private record Occluders(List<AABB> boxes, List<double[]> sprites) {
        boolean isEmpty() {
            return boxes.isEmpty() && sprites.isEmpty();
        }

        int size() {
            return boxes.size() + sprites.size();
        }
    }

    private static Occluders occluders(final Frustum frustum, final Vec3 projectorPos) {
        final Minecraft minecraft = Minecraft.getInstance();
        final List<AABB> boxes = new ArrayList<>();
        final List<double[]> sprites = new ArrayList<>();
        final Occluders result = new Occluders(boxes, sprites);
        if (minecraft.level == null) return result;
        final double maxDistanceSqr = (double) ProjectorBlockEntity.MAX_RENDER_DISTANCE
                * ProjectorBlockEntity.MAX_RENDER_DISTANCE;
        for (final Entity entity : minecraft.level.entitiesForRendering()) {
            if (result.size() >= MAX_OCCLUDER_BOXES) break;
            if (entity.isInvisible() || entity.isSpectator()) continue;
            addOccluder(boxes, frustum, projectorPos, maxDistanceSqr, entity.getBoundingBox());
        }
        final var particles = ((ParticleEngineAccessor) minecraft.particleEngine).oc2r$getParticles();
        for (final var group : particles.values()) {
            for (final var particle : group.getAll()) {
                if (result.size() >= MAX_OCCLUDER_BOXES) return result;
                final AABB box = particle.getBoundingBox();
                final double half = particle instanceof SingleQuadParticle quad
                        ? quad.getQuadSize(1f) : box.getXsize() / 2;
                final Vec3 center = box.getCenter();
                if (center.distanceToSqr(projectorPos) > maxDistanceSqr) continue;
                if (!frustum.isVisible(new AABB(center, center).inflate(half))) continue;
                sprites.add(new double[] {center.x, center.y, center.z, half});
            }
        }
        return result;
    }

    private static void addOccluder(final List<AABB> boxes, final Frustum frustum,
            final Vec3 projectorPos, final double maxDistanceSqr, final AABB box) {
        if (box.getCenter().distanceToSqr(projectorPos) > maxDistanceSqr) return;
        if (frustum.isVisible(box)) boxes.add(box);
    }

    private static void drawOccluders(final RenderPass pass, final Occluders occluders,
            final Vec3 projectorPos, final Vec3 cameraPos, final GpuBuffer matrices) {
        if (occluders.isEmpty()) return;
        final List<AABB> boxes = occluders.boxes();
        final var device = RenderSystem.getDevice();
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(occluders.size() * 24 * 12)) {
            final BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            for (final AABB box : boxes) addBox(builder, box.move(-cameraPos.x, -cameraPos.y, -cameraPos.z));
            for (final double[] sprite : occluders.sprites()) addSprite(builder, sprite, projectorPos, cameraPos);
            try (MeshData mesh = builder.buildOrThrow();
                    GpuBuffer vertices = device.createBuffer(
                            () -> "oc2r projector occluders", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())) {
                final int indexCount = mesh.drawState().indexCount();
                final var indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                pass.setPipeline(DEPTH_BOXES);
                pass.setUniform("BoxMatrices", matrices);
                pass.setVertexBuffer(0, vertices);
                pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type());
                pass.drawIndexed(0, 0, indexCount, 1);
            }
        }
    }

    // A flat quad facing the projector keeps a particle's shadow the size of the particle itself;
    // a cube would cover up to 1.7 times that from an oblique angle.
    private static void addSprite(final BufferBuilder b, final double[] sprite,
            final Vec3 projectorPos, final Vec3 cameraPos) {
        final Vec3 center = new Vec3(sprite[0], sprite[1], sprite[2]);
        final Vec3 forward = center.subtract(projectorPos).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
        right = right.normalize().scale(sprite[3]);
        final Vec3 up = forward.cross(right).normalize().scale(sprite[3]);
        final Vec3 c = center.subtract(cameraPos);
        quad(b,
                (float) (c.x - right.x - up.x), (float) (c.y - right.y - up.y), (float) (c.z - right.z - up.z),
                (float) (c.x + right.x - up.x), (float) (c.y + right.y - up.y), (float) (c.z + right.z - up.z),
                (float) (c.x + right.x + up.x), (float) (c.y + right.y + up.y), (float) (c.z + right.z + up.z),
                (float) (c.x - right.x + up.x), (float) (c.y - right.y + up.y), (float) (c.z - right.z + up.z));
    }

    private static void addBox(final BufferBuilder b, final AABB box) {
        final float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
        final float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
        quad(b, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(b, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(b, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(b, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(b, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(b, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(final BufferBuilder b, final float... c) {
        for (int i = 0; i < 12; i += 3) b.addVertex(c[i], c[i + 1], c[i + 2]);
    }

    private static void renderDepth(final ChunkSectionsToRender chunks, final Occluders occluders,
            final Vec3 projectorPos, final Vec3 cameraPos, final Matrix4f projectorCamera) {
        final var device = RenderSystem.getDevice();
        device.createCommandEncoder().clearDepthTexture(depthTarget.getDepthTexture(), 1.0);
        try (MemoryStack stack = MemoryStack.stackPush();
                GpuBuffer projection = device.createBuffer(() -> "oc2r projector projection",
                        GpuBuffer.USAGE_UNIFORM, Std140Builder.onStack(stack, 64).putMat4f(PROJECTION).get());
                GpuBuffer boxMatrices = device.createBuffer(() -> "oc2r projector box matrices",
                        GpuBuffer.USAGE_UNIFORM, Std140Builder.onStack(stack, 64).putMat4f(projectorCamera).get());
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
            drawOccluders(pass, occluders, projectorPos, cameraPos, boxMatrices);
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
