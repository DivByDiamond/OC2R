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
    private static final float FRUSTUM_X = (ProjectorBlockEntity.MAX_WIDTH - 1) / 2 / FRUSTUM_SCALE;
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
            final SectionRenderDispatcher.RenderSection[] sections) { // NOPMD - This parameter is a complete fixed-shape array, not a variable argument list.
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
                final Matrix4f projectorView = new Matrix4f() // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                        .rotationY((float) Math.toRadians(entry.yRot() + 180))
                        .translate((float) (camera.pos.x - entry.worldPos().x),
                                (float) (camera.pos.y - entry.worldPos().y),
                                (float) (camera.pos.z - entry.worldPos().z));
                final Matrix4f projectorCamera = new Matrix4f(PROJECTION).mul(projectorView); // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                final Frustum frustum = new Frustum(// NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                        new Matrix4f().rotationY((float) Math.toRadians(entry.yRot() + 180)), PROJECTION); // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                frustum.prepare(entry.worldPos().x, entry.worldPos().y, entry.worldPos().z);
                visible.clear();
                for (final var section : sections) {
                    if (section != null && frustum.isVisible(section.getBoundingBox())) visible.add(section);
                }
                final ChunkSectionsToRender chunks = renderer.prepareChunkRenders(projectorView);
                renderDepth(chunks, occluders(frustum, entry.worldPos()), entry.worldPos(), camera.pos, projectorCamera);
                ProjectorBlockEntityDepth.render(depthTarget, projectorCamera, visible, frustum, entry.worldPos(), camera);
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
                if (!frustum.isVisible(new AABB(center, center).inflate(half))) continue; // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
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
        final float x0 = (float) box.minX;
        final float y0 = (float) box.minY;
        final float z0 = (float) box.minZ;
        final float x1 = (float) box.maxX;
        final float y1 = (float) box.maxY;
        final float z1 = (float) box.maxZ;
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

//? if >=26.2 {
/*
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutionException;
import li.cil.oc2.api.API;
import li.cil.oc2.client.mixin.ParticleEngineAccessor;
import li.cil.oc2.client.mixin.ParticleGroupAccessor;
import li.cil.oc2.client.renderer.stage.RenderInfo;
import li.cil.oc2.client.renderer.stage.shader.ModShaders;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.misc.ProjectorDevice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

// GPU depth and compositing passes for the 26.2 level frame graph. Same structure as the 26.1
// renderer, ported to bind groups and to the reversed-Z depth convention of 26.2: depth buffers
// are cleared to 0.0 (far), a larger value is closer, and the depth test is GREATER_THAN_OR_EQUAL
// (DepthStencilState.DEFAULT).
public final class ProjectorPipelineRenderer {
    private static final int UNIFORM_SIZE = 160;
    private static final float FRUSTUM_SCALE =
            (ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE + 4f)
                    * ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE;
    private static final float FRUSTUM_X = (ProjectorBlockEntity.MAX_WIDTH - 1) / 2 / FRUSTUM_SCALE;
    private static final float FRUSTUM_Y = (ProjectorBlockEntity.MAX_HEIGHT - 1) / FRUSTUM_SCALE;
    private static final float NEAR = 1f / 16f;
    // Conventional projection, only used to cull sections and occluders against the projector.
    private static final Matrix4f CULL_PROJECTION = new Matrix4f().frustum(
            -FRUSTUM_X, FRUSTUM_X, 0, FRUSTUM_Y, NEAR, ProjectorBlockEntity.MAX_RENDER_DISTANCE);
    private static final BindGroupLayout BOX_MATRICES = BindGroupLayout.builder()
            .withUniform("BoxMatrices", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout COMPOSITE_INPUTS = BindGroupLayout.builder()
            .withSampler("MainDepth").withSampler("ProjectorDepth").withSampler("ProjectorColor")
            .withUniform("ProjectorMatrices", UniformType.UNIFORM_BUFFER).build();
    private static final ColorTargetState NO_COLOR_WRITES =
            new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE);
    private static final RenderPipeline DEPTH_SOLID = depthPipeline(false);
    private static final RenderPipeline DEPTH_CUTOUT = depthPipeline(true);
    // Entities and particles are not part of the chunk meshes; they cast shadows as boxes.
    private static final RenderPipeline DEPTH_BOXES = RenderPipeline.builder()
            .withLocation(id("pipeline/projector_depth_boxes"))
            .withVertexShader(id("core/projector_depth_box"))
            .withFragmentShader(id("core/projector_depth_box"))
            .withBindGroupLayout(BOX_MATRICES)
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withColorTargetState(NO_COLOR_WRITES)
            .withCull(false).build();
    private static final int MAX_OCCLUDER_BOXES = 2048;
    private static final RenderPipeline COMPOSITE = RenderPipeline.builder()
            .withLocation(id("pipeline/projector_composite"))
            .withVertexShader(id("core/projector_composite"))
            .withFragmentShader(id("core/projector_composite_reversed"))
            .withBindGroupLayout(COMPOSITE_INPUTS)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withCull(false).build();
    private static final Cache<ProjectorBlockEntity, RenderInfo> FRAMES = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(5))
            .removalListener(ProjectorPipelineRenderer::releaseFrame).build();
    private static RenderTarget depthTarget;
    private static Matrix4fc mainProjection;
    private static Matrix4f projection;
    private static boolean zZeroToOne;

    public static void setMainProjection(final Matrix4fc projection) {
        mainProjection = projection;
    }

    private ProjectorPipelineRenderer() {}

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(API.MOD_ID, path);
    }

    private static RenderPipeline depthPipeline(final boolean cutout) {
        final var builder = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(id("pipeline/projector_depth_" + (cutout ? "cutout" : "solid")))
                .withVertexShader(id("core/projector_depth"))
                .withFragmentShader(id("core/projector_depth"))
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION)
                .withVertexBinding(0, DefaultVertexFormat.BLOCK)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withColorTargetState(NO_COLOR_WRITES)
                .withCull(true);
        if (cutout) builder.withShaderDefine("CUTOUT").withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        return builder.build();
    }

    // The projector frustum in the depth convention of the device: near maps to 1.0 and far to 0.0
    // when clip depth is [0, 1] (the vanilla default); otherwise to 1.0 and -1.0.
    private static Matrix4f projection() {
        if (projection == null) {
            zZeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
            final Matrix4f conventional = new Matrix4f().frustum(
                    -FRUSTUM_X, FRUSTUM_X, 0, FRUSTUM_Y, NEAR,
                    ProjectorBlockEntity.MAX_RENDER_DISTANCE, zZeroToOne);
            final Matrix4f reversed = new Matrix4f(conventional);
            for (int column = 0; column < 4; column++) {
                // Reversed-Z replaces clip z by w - z (or by -z for a [-1, 1] range).
                reversed.set(column, 2, zZeroToOne
                        ? conventional.get(column, 3) - conventional.get(column, 2)
                        : -conventional.get(column, 2));
            }
            projection = reversed;
        }
        return projection;
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

    // Every section of the view area, not only the ones visible to the player camera: the
    // projector can look somewhere the player does not.
    private static List<SectionRenderDispatcher.RenderSection> allSections(final ViewArea area) {
        final List<SectionRenderDispatcher.RenderSection> sections = new ArrayList<>();
        final SectionPos center = area.getCameraSectionPos();
        final int radius = area.getViewDistance();
        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                for (int y = area.minSectionY(); y <= area.maxSectionY(); y++) {
                    final var section = area.getRenderSectionAt(new BlockPos(x << 4, y << 4, z << 4)); // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                    if (section != null) sections.add(section);
                }
            }
        }
        return sections;
    }

    public static void render(
            final LevelRenderer renderer, final CameraRenderState camera, final Matrix4fc mainView) {
        if (ProjectorDepthRenderer.VISIBLE_PROJECTORS.isEmpty()) return;
        final ViewArea area = renderer.viewArea();
        if (area == null) {
            ProjectorDepthRenderer.VISIBLE_PROJECTORS.clear();
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final RenderTarget main = minecraft.gameRenderer.mainRenderTarget();
        final var visible = renderer.visibleSections();
        final List<SectionRenderDispatcher.RenderSection> savedSections = new ArrayList<>(visible);
        final List<SectionRenderDispatcher.RenderSection> sections = allSections(area);
        final List<ProjectorDepthRenderer.VisibleProjector> projectors =
                new ArrayList<>(ProjectorDepthRenderer.VISIBLE_PROJECTORS);
        ProjectorDepthRenderer.VISIBLE_PROJECTORS.clear();
        projectors.sort(Comparator.comparingDouble(entry -> entry.worldPos().distanceToSqr(camera.pos)));
        if (depthTarget == null) {
            depthTarget = new TextureTarget("oc2r projector depth", 512, 512, true, GpuFormat.RGBA8_UNORM);
        }
        final Matrix4f projectorProjection = projection();
        final Matrix4f inverseMain = new Matrix4f(mainProjection != null ? mainProjection : camera.projectionMatrix)
                .mul(mainView).invert();
        try {
            for (int index = 0; index < Math.min(projectors.size(), ModShaders.MAX_PROJECTORS); index++) {
                final var entry = projectors.get(index);
                if (entry.projector().isRemoved() || !entry.projector().hasEnergy()) continue;
                final Matrix4f rotation = new Matrix4f().rotationY((float) Math.toRadians(entry.yRot() + 180)); // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                final Matrix4f projectorView = new Matrix4f(rotation) // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                        .translate((float) (camera.pos.x - entry.worldPos().x),
                                (float) (camera.pos.y - entry.worldPos().y),
                                (float) (camera.pos.z - entry.worldPos().z));
                final Matrix4f projectorCamera = new Matrix4f(projectorProjection).mul(projectorView); // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                final Frustum frustum = new Frustum(rotation, CULL_PROJECTION); // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                frustum.prepare(entry.worldPos().x, entry.worldPos().y, entry.worldPos().z);
                visible.clear();
                for (final var section : sections) {
                    if (frustum.isVisible(section.getBoundingBox())) visible.add(section);
                }
                final ChunkSectionsToRender chunks = renderer.prepareChunkRenders(projectorView);
                renderDepth(chunks, occluders(frustum, entry.worldPos()), entry.worldPos(), camera.pos, projectorCamera);
                ProjectorBlockEntityDepth.render(depthTarget, projectorCamera, visible, frustum, entry.worldPos(), camera);
                final RenderInfo frame = frame(entry.projector());
                entry.projector().onRendering();
                // Direction from a surface towards the projector: the view space +z axis in world space.
                final Vector3f axis = rotation.transpose(new Matrix4f()).transformDirection(new Vector3f(0, 0, 1)); // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
                composite(main, inverseMain, projectorCamera, axis, frame);
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
            for (final Particle particle : ((ParticleGroupAccessor) group).oc2r$getParticles()) {
                if (result.size() >= MAX_OCCLUDER_BOXES) return result;
                final AABB box = particle.getBoundingBox();
                final double half = particle instanceof SingleQuadParticle quad
                        ? quad.getQuadSize(1f) : box.getXsize() / 2;
                final Vec3 center = box.getCenter();
                if (center.distanceToSqr(projectorPos) > maxDistanceSqr) continue;
                if (!frustum.isVisible(new AABB(center, center).inflate(half))) continue; // NOPMD - Each iteration owns distinct geometry, coordinates or serialized state.
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

    // Vertex data of the occluders, kept alive until the render pass that draws it has ended.
    private record OccluderMesh(GpuBuffer vertices, int indexCount) implements AutoCloseable {
        @Override
        public void close() {
            vertices.close();
        }
    }

    private static OccluderMesh buildOccluders(
            final Occluders occluders, final Vec3 projectorPos, final Vec3 cameraPos) {
        if (occluders.isEmpty()) return null;
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(occluders.size() * 24 * 12)) {
            final BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
            for (final AABB box : occluders.boxes()) addBox(builder, box.move(-cameraPos.x, -cameraPos.y, -cameraPos.z));
            for (final double[] sprite : occluders.sprites()) addSprite(builder, sprite, projectorPos, cameraPos);
            try (MeshData mesh = builder.buildOrThrow()) {
                final GpuBuffer vertices = RenderSystem.getDevice().createBuffer(
                        () -> "oc2r projector occluders", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                return new OccluderMesh(vertices, mesh.drawState().indexCount());
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
        final float x0 = (float) box.minX;
        final float y0 = (float) box.minY;
        final float z0 = (float) box.minZ;
        final float x1 = (float) box.maxX;
        final float y1 = (float) box.maxY;
        final float z1 = (float) box.maxZ;
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
        // Reversed-Z: the far plane is 0.0.
        device.createCommandEncoder().clearDepthTexture(depthTarget.getDepthTexture(), 0.0);
        try (MemoryStack stack = MemoryStack.stackPush();
                GpuBuffer projectionBuffer = device.createBuffer(() -> "oc2r projector projection",
                        GpuBuffer.USAGE_UNIFORM, Std140Builder.onStack(stack, 64).putMat4f(projection()).get());
                GpuBuffer boxMatrices = device.createBuffer(() -> "oc2r projector box matrices",
                        GpuBuffer.USAGE_UNIFORM, Std140Builder.onStack(stack, 64).putMat4f(projectorCamera).get());
                OccluderMesh occluderMesh = buildOccluders(occluders, projectorPos, cameraPos);
                RenderPass pass = device.createCommandEncoder().createRenderPass(
                        () -> "oc2r projector terrain depth", depthTarget.getColorTextureView(),
                        Optional.empty(), depthTarget.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setUniform("Globals", RenderSystem.getGlobalSettingsUniform());
            pass.setUniform("Projection", projectionBuffer);
            final var indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            // One shared index buffer for the terrain and the occluders, so it is not reallocated mid-pass.
            final int indicesRequired = Math.max(chunks.maxIndicesRequired(),
                    occluderMesh == null ? 0 : occluderMesh.indexCount());
            final GpuBuffer indexBuffer = indicesRequired == 0 ? null : indices.getBuffer(indicesRequired);
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
            if (occluderMesh != null) {
                final int indexCount = occluderMesh.indexCount();
                pass.setPipeline(DEPTH_BOXES);
                pass.setUniform("BoxMatrices", boxMatrices);
                pass.setVertexBuffer(0, occluderMesh.vertices().slice());
                pass.setIndexBuffer(indexBuffer, indices.type());
                pass.drawIndexed(indexCount, 1, 0, 0, 0);
            }
        }
    }

    private static void composite(final RenderTarget main, final Matrix4f inverseMain,
            final Matrix4f projectorCamera, final Vector3f projectorAxis, final RenderInfo frame) {
        final var device = RenderSystem.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush();
                GpuBuffer matrices = device.createBuffer(() -> "oc2r projector matrices", GpuBuffer.USAGE_UNIFORM,
                        Std140Builder.onStack(stack, UNIFORM_SIZE).putMat4f(inverseMain).putMat4f(projectorCamera)
                                .putVec4(projectorAxis.x, projectorAxis.y, projectorAxis.z, 0f)
                                .putVec4(zZeroToOne ? 1f : 0f, 0f, 0f, 0f).get());
                RenderPass pass = device.createCommandEncoder().createRenderPass(
                        () -> "oc2r projector composite", main.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(COMPOSITE);
            pass.setUniform("ProjectorMatrices", matrices);
            final var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            pass.bindTexture("MainDepth", main.getDepthTextureView(), sampler);
            pass.bindTexture("ProjectorDepth", depthTarget.getDepthTextureView(), sampler);
            pass.bindTexture("ProjectorColor", frame.getTexture().getTextureView(), sampler);
            pass.draw(3, 1, 0, 0);
        }
    }
}
*///?}
