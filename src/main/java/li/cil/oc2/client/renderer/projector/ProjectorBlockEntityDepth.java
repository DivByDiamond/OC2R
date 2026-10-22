package li.cil.oc2.client.renderer.projector;

//? if >=26.1 {
/*
//? if >=26.2 {
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
//?}
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
//? if >=26.2 {
import com.mojang.blaze3d.pipeline.BindGroupLayout;
//?}
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
//? if <26.2 {
import com.mojang.blaze3d.vertex.VertexFormat;
//?}
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
//? if <26.2 {
import java.util.OptionalInt;
//?}
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import li.cil.oc2.api.API;
//? if <26.2 {
import li.cil.oc2.client.mixin.ProjectorRenderTypeAccessor;
//?}
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
//? if <26.2 {
import net.minecraft.client.model.geom.ModelPart;
//?}
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

// Depth from actual animated model, model-part and custom-quad submissions, outside chunk meshes.
// Blended effects, text, item/moving-block submissions and non-quad custom pipelines are not
// opaque occluders in this pass. No replacement bounds are emitted for unsupported geometry.
public final class ProjectorBlockEntityDepth {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_BLOCK_ENTITIES = 512;
    // Vertex budget of one depth pass. The collector is created per call, so the budget is
    // reset for every projector and frame; the batches of a pass share it.
    private static final int MAX_DEPTH_VERTICES = 262144;
    // Latch for the truncation warning: the collector is recreated every frame, so a plain
    // flag would repeat the warning in the log on every frame.
    private static final AtomicBoolean TRUNCATION_REPORTED = new AtomicBoolean();
    private static final RenderPipeline PIPELINE = pipeline();

    private ProjectorBlockEntityDepth() {}

    private static RenderPipeline pipeline() {
        final var builder = RenderPipeline.builder()
                .withLocation(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "pipeline/projector_model_depth"))
                .withVertexShader(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "core/projector_model_depth"))
                .withFragmentShader(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "core/projector_model_depth"))
                .withDepthStencilState(DepthStencilState.DEFAULT).withCull(false);
        //? if >=26.2 {
        builder.withBindGroupLayout(BindGroupLayout.builder()
                        .withUniform("ModelMatrices", UniformType.UNIFORM_BUFFER).withSampler("Sampler0").build())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE));
        //?} else {
        builder.withUniform("ModelMatrices", UniformType.UNIFORM_BUFFER).withSampler("Sampler0")
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_NONE));
        //?}
        return builder.build();
    }

    public static void render(final RenderTarget target, final Matrix4fc projectorCamera,
            final Iterable<SectionRenderDispatcher.RenderSection> sections, final Frustum frustum,
            final Vec3 projectorPos, final CameraRenderState camera) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        try (Collector collector = new Collector()) {
            final Set<BlockEntity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (final var section : sections) {
                for (final BlockEntity entity : section.getSectionMesh().getRenderableBlockEntities()) {
                    if (seen.size() >= MAX_BLOCK_ENTITIES) break;
                    if (seen.add(entity)) submit(entity, collector, frustum, projectorPos, camera, partialTick);
                }
            }
            for (final BlockEntity entity : minecraft.level.getGloballyRenderedBlockEntities()) {
                if (seen.size() >= MAX_BLOCK_ENTITIES) break;
                if (seen.add(entity)) submit(entity, collector, frustum, projectorPos, camera, partialTick);
            }
            collector.draw(target, projectorCamera);
        }
    }

    private static <E extends BlockEntity, S extends BlockEntityRenderState> void submit(
            final E entity, final Collector collector, final Frustum frustum, final Vec3 projectorPos,
            final CameraRenderState camera, final float partialTick) {
        if (entity instanceof ProjectorBlockEntity || entity.isRemoved() || !entity.hasLevel() || !entity.getType().isValid(entity.getBlockState())) return;
        final BlockEntityRenderer<E, S> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null || !renderer.shouldRender(entity, projectorPos)
                || !frustum.isVisible(renderer.getRenderBoundingBox(entity))) return;
        if (new Vec3(entity.getBlockPos().getX() + 0.5, entity.getBlockPos().getY() + 0.5,
                entity.getBlockPos().getZ() + 0.5).distanceToSqr(projectorPos)
                > (double) ProjectorBlockEntity.MAX_RENDER_DISTANCE * ProjectorBlockEntity.MAX_RENDER_DISTANCE) return;
        final S state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, partialTick, projectorPos, null);
        final PoseStack pose = new PoseStack();
        pose.translate(state.blockPos.getX() - camera.pos.x, state.blockPos.getY() - camera.pos.y, state.blockPos.getZ() - camera.pos.z);
        renderer.submit(state, pose, collector, camera);
    }

    // Models submit synchronously so their mutable animation state cannot be overwritten by another instance.
    static final class Collector extends SubmitNodeStorage implements AutoCloseable {
        private int vertexCount;
        private final Map<RenderType, Batch> batches = new IdentityHashMap<>(); // NOPMD - Map ownership is confined to one thread or protected by its explicit monitor.
        private final SubmitNodeCollection collection = new SubmitNodeCollection(
                //? if <26.2 {
                this
                //?}
        ) {
            @Override
            public <S> void submitModel(final Model<? super S> model, final S state, final PoseStack pose,
                    final RenderType type, final int light, final int overlay, final int color,
                    final TextureAtlasSprite sprite, final int outline, final ModelFeatureRenderer.CrumblingOverlay breaking) {
                final VertexConsumer buffer = buffer(type);
                if (buffer == null) return;
                model.setupAnim(state);
                model.renderToBuffer(pose, sprite == null ? buffer : sprite.wrap(buffer), light, overlay, color);
            }

            //? if <26.2 {
            @Override
            public void submitModelPart(final ModelPart model, final PoseStack pose, final RenderType type,
                    final int light, final int overlay, final TextureAtlasSprite sprite,
                    final boolean sheeted, final boolean foil, final int color,
                    final ModelFeatureRenderer.CrumblingOverlay breaking, final int outline) {
                final VertexConsumer buffer = buffer(type);
                if (buffer != null) model.render(pose, sprite == null ? buffer : sprite.wrap(buffer), light, overlay, color);
            }
            //?}

            @Override
            public void submitCustomGeometry(final PoseStack pose, final RenderType type,
                    final SubmitNodeCollector.CustomGeometryRenderer geometry) {
                final VertexConsumer buffer = buffer(type);
                if (buffer != null) geometry.render(pose.last(), buffer);
            }
        };

        @Override
        public SubmitNodeCollection order(final int order) {
            return collection;
        }

        private VertexConsumer buffer(final RenderType type) {
            if (type.hasBlending() || type.isOutline()) return null;
            final var depth = type.pipeline().getDepthStencilState();
            if (depth == null || !depth.writeDepth()) return null;
            // Other topologies cannot share the quad index buffer and are deliberately not reinterpreted.
            //? if >=26.2 {
            if (type.primitiveTopology() != PrimitiveTopology.QUADS) return null;
            //?} else {
            if (type.mode() != VertexFormat.Mode.QUADS) return null;
            //?}
            return batches.computeIfAbsent(type, ignored -> new Batch(this)).consumer;
        }

        MeshData finish(final RenderType type) {
            final Batch batch = batches.get(type);
            return batch == null ? null : batch.finish();
        }

        void draw(final RenderTarget target, final Matrix4fc projectorCamera) {
            for (final var entry : batches.entrySet()) {
                final RenderType type = entry.getKey();
                final float cutoff = Float.parseFloat(type.pipeline().getShaderDefines().values().getOrDefault("ALPHA_CUTOUT", "-1"));
                final Texture texture = texture(type);
                if (texture == null) continue;
                try (MeshData mesh = finish(type)) {
                    if (mesh == null) continue;
                    final var device = RenderSystem.getDevice();
                    final var indices = RenderSystem.getSequentialBuffer(
                            //? if >=26.2 {
                            PrimitiveTopology.QUADS
                            //?} else {
                            VertexFormat.Mode.QUADS
                            //?}
                    );
                    try (MemoryStack stack = MemoryStack.stackPush();
                            GpuBuffer matrices = device.createBuffer(() -> "oc2r projector model matrices", GpuBuffer.USAGE_UNIFORM,
                                    Std140Builder.onStack(stack, 80).putMat4f(projectorCamera).putVec4(cutoff, 0, 0, 0).get());
                            GpuBuffer vertices = device.createBuffer(() -> "oc2r projector model vertices", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                            RenderPass pass = device.createCommandEncoder().createRenderPass(() -> "oc2r projector block entity depth",
                                    target.getColorTextureView(),
                                    //? if >=26.2 {
                                    Optional.empty(),
                                    //?} else {
                                    OptionalInt.empty(),
                                    //?}
                                    target.getDepthTextureView(), OptionalDouble.empty())) {
                        pass.setPipeline(PIPELINE);
                        pass.setUniform("ModelMatrices", matrices);
                        pass.bindTexture("Sampler0", texture.view(), texture.sampler());
                        final int count = mesh.drawState().indexCount();
                        pass.setIndexBuffer(indices.getBuffer(count), indices.type());
                        //? if >=26.2 {
                        pass.setVertexBuffer(0, vertices.slice());
                        pass.drawIndexed(count, 1, 0, 0, 0);
                        //?} else {
                        pass.setVertexBuffer(0, vertices);
                        pass.drawIndexed(0, 0, count, 1);
                        //?}
                    }
                }
            }
        }

        @Override
        public void close() {
            batches.values().forEach(batch -> batch.bytes.close());
        }
    }

    private record Texture(GpuTextureView view, GpuSampler sampler) {}

    private static Texture texture(final RenderType type) {
        //? if >=26.2 {
        for (final var texture : type.prepare().textures()) {
            if ("Sampler0".equals(texture.name())) return new Texture(texture.textureView(), texture.sampler());
        }
        //?} else {
        final var texture = ((ProjectorRenderTypeAccessor) (Object) type).oc2r$getSetup().getTextures().get("Sampler0");
        if (texture != null) return new Texture(texture.textureView(), texture.sampler());
        //?}
        return null;
    }

    private static final class Batch {
        private final Collector collector;
        private final ByteBufferBuilder bytes = new ByteBufferBuilder(4096);
        private final BufferBuilder builder = new BufferBuilder(bytes,
                //? if >=26.2 {
                PrimitiveTopology.QUADS,
                //?} else {
                VertexFormat.Mode.QUADS,
                //?}
                DefaultVertexFormat.POSITION_TEX_COLOR);
        private boolean pending;
        private float x;
        private float y;
        private float z;
        private float u;
        private float v;
        private int color = -1;

        private Batch(final Collector collector) {
            this.collector = collector;
        }

        private void flushVertex() {
            if (pending) {
                if (collector.vertexCount < MAX_DEPTH_VERTICES) {
                    builder.addVertex(x, y, z).setUv(u, v).setColor(color);
                    collector.vertexCount++;
                } else if (TRUNCATION_REPORTED.compareAndSet(false, true)) {
                    // The dropped vertices leave holes in the depth pass, which shows up as
                    // wrong occlusion; report it once instead of failing silently.
                    LOGGER.warn(
                            "Projector depth pass dropped geometry: the vertex budget of {} was reached",
                            MAX_DEPTH_VERTICES);
                }
            }
            pending = false;
        }

        private MeshData finish() {
            flushVertex();
            return builder.build();
        }

        private final VertexConsumer consumer = new VertexConsumer() {
            @Override public VertexConsumer addVertex(final float px, final float py, final float pz) {
                flushVertex();
                x = px;
                y = py;
                z = pz;
                u = 0;
                v = 0;
                color = -1;
                pending = true;
                return this;
            }

            @Override public VertexConsumer setColor(final int r, final int g, final int b, final int a) {
                color = (a << 24) | (r << 16) | (g << 8) | b;
                return this;
            }

            @Override public VertexConsumer setColor(final int value) {
                color = value;
                return this;
            }

            @Override public VertexConsumer setUv(final float pu, final float pv) {
                u = pu;
                v = pv;
                return this;
            }

            @Override public VertexConsumer setUv1(final int u, final int v) {
                return this;
            }

            @Override public VertexConsumer setUv2(final int u, final int v) {
                return this;
            }

            @Override public VertexConsumer setNormal(final float x, final float y, final float z) {
                return this;
            }

            @Override public VertexConsumer setLineWidth(final float width) {
                return this;
            }
        };
    }
}
*///?}
