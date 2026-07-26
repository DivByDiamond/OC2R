/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import li.cil.oc2.api.API;
import net.minecraft.client.DeltaTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix4f;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.ProjectorDevice;
import li.cil.oc2.common.ext.MinecraftExt;
import li.cil.oc2.common.mixin.LevelRendererMixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@EventBusSubscriber(modid = API.MOD_ID, value=Dist.CLIENT)
public final class ProjectorDepthRenderer {
    private static final int DEPTH_CAPTURE_SIZE = 256;

    /**
     * A projector that is visible this frame, paired with the world-space
     * position it is being rendered at and the world-space Y-rotation its
     * lens is pointing. For a regular block the world position is just
     * BlockPos.center() and the Y-rotation is blockState.FACING.toYRot();
     * for a Sable / Create:Aeronautics contraption block the raw BlockPos is
     * a far-away virtual coordinate AND the blockState FACING doesn't account
     * for the contraption's rotation, so the BER computes both the actual
     * position and the actual Y-rotation from its PoseStack and passes them
     * here. The depth-camera setup needs both to know where to place its
     * frustum and which way to aim it.
     */
    private record VisibleProjector(ProjectorBlockEntity projector, Vec3 worldPos, float yRot) {}

    private static final List<VisibleProjector> VISIBLE_PROJECTORS = new ArrayList<>();
    private static final DepthOnlyRenderTarget[] PROJECTOR_DEPTH_TARGETS = new DepthOnlyRenderTarget[ModShaders.MAX_PROJECTORS];
    private static final DynamicTexture[] PROJECTOR_COLOR_TARGETS = new DynamicTexture[ModShaders.MAX_PROJECTORS];
    private static final Matrix4f[] PROJECTOR_CAMERA_MATRICES = new Matrix4f[ModShaders.MAX_PROJECTORS];
    private static final Camera PROJECTOR_DEPTH_CAMERA = new Camera();
    private static DepthOnlyRenderTarget MAIN_CAMERA_DEPTH = null;
    private static final float PROJECTOR_FORWARD_SHIFT = 7 / 16f; // From center of projector block.
    private static final float PROJECTOR_NEAR = 0.5f - PROJECTOR_FORWARD_SHIFT;
    private static final float PROJECTOR_FAR = ProjectorBlockEntity.MAX_RENDER_DISTANCE;
    private static final int FRUSTUM_WIDTH = (ProjectorBlockEntity.MAX_WIDTH - 1) / 2;
    private static final int FRUSTUM_HEIGHT = ProjectorBlockEntity.MAX_HEIGHT - 1;
    private static final Matrix4f DEPTH_CAMERA_PROJECTION_MATRIX = (new Matrix4f()).frustum(-calculateFrustumComponent(FRUSTUM_WIDTH), calculateFrustumComponent(FRUSTUM_WIDTH), 0, calculateFrustumComponent(FRUSTUM_HEIGHT), PROJECTOR_NEAR, PROJECTOR_FAR);

    private static final Cache<ProjectorBlockEntity, ProjectorDepthRenderInfo> RENDER_INFO = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(ProjectorDepthRenderer::handleProjectorNoLongerRendering)
        .build();

    private static boolean isRenderingProjectorDepth;
    private static HitResult hitResultBak;
    private static boolean entityShadowsBak;
    private static Entity minecraftCameraEntityBak;

    static {
        RenderSystem.recordRenderCall(() -> {
            MAIN_CAMERA_DEPTH = new DepthOnlyRenderTarget(MainTarget.DEFAULT_WIDTH, MainTarget.DEFAULT_HEIGHT);

            for (int i = 0; i < ModShaders.MAX_PROJECTORS; i++) {
                PROJECTOR_DEPTH_TARGETS[i] = new DepthOnlyRenderTarget(DEPTH_CAPTURE_SIZE, DEPTH_CAPTURE_SIZE);
                PROJECTOR_CAMERA_MATRICES[i] = new Matrix4f();
            }
        });
    }

    private static float calculateFrustumComponent(float originalValue)
    {
        return (originalValue / (ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE + 4f)) / ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE;
    }

    private static void handleProjectorNoLongerRendering(final RemovalNotification<ProjectorBlockEntity, ProjectorDepthRenderInfo> notification) {
        final ProjectorBlockEntity projector = notification.getKey();
        if (projector != null) {
            projector.setFrameConsumer(null);
        }
        final ProjectorDepthRenderInfo renderInfo = notification.getValue();
        if (renderInfo != null) {
            renderInfo.close();
        }
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Adds a projector that is being rendered this frame. This is called every frame a projector is rendering,
     * the list of rendering projectors is cleared at the end of every frame.
     *
     * @param projector the projector BlockEntity to render. For a Sable / Create:Aeronautics
     *                 contraption this should be the primary BlockEntity (same persistent
     *                 device id, real BlockPos) so the framebuffer has actual image data;
     *                 the virtual clone's framebuffer never receives updates.
     * @param worldPos the world-space position of the projector's lens (block
     *                 center + forward shift). For a contraption block this
     *                 MUST be computed from the BER's PoseStack — the raw
     *                 BlockPos is a far-away virtual coordinate unsuitable
     *                 for depth-camera setup.
     * @param yRot     the world-space Y-rotation the projector's lens is
     *                 pointing towards (MC convention: 0=south, 90=west,
     *                 180=north, 270=east). For a contraption block this
     *                 MUST be derived from the BER's PoseStack light
     *                 direction — blockState.FACING.toYRot() doesn't account
     *                 for the contraption's rotation.
     */
    public static void addProjector(final ProjectorBlockEntity projector, final Vec3 worldPos, final float yRot) {
        VISIBLE_PROJECTORS.add(new VisibleProjector(projector, worldPos, yRot));
    }

    /**
     * Whether we will be rendering projector depth this frame.
     * <p>
     * Checked in our {@link LevelRendererMixin} to avoid unnecessary flushing and copying
     * when we're not rendering projections.
     */
    public static boolean willRenderProjectorDepth() {
        return !VISIBLE_PROJECTORS.isEmpty();
    }

    /**
     * Whether we're currently rendering projector depth maps.
     * <p>
     * This is used in a couple of events and mixins, used to suppress regular rendering of things not needed in the
     * depth buffer.
     */
    public static boolean isIsRenderingProjectorDepth() {
        return isRenderingProjectorDepth;
    }

    /**
     * Called from a mixin in the {@link LevelRenderer#renderLevel(PoseStack, float, long, boolean, Camera, GameRenderer, LightTexture, Matrix4f)}
     * method to grab the current depth buffer. This is necessary, because the depth buffer may be messed up by other
     * render passes when using the "Fabulous!" graphics mode.
     * <p>
     * Called before {@link #renderProjectors(RenderLevelStageEvent)} every frame.
     */
    public static void captureMainCameraDepth() {
        final RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
        if (mainRenderTarget.width != MAIN_CAMERA_DEPTH.width || mainRenderTarget.height != MAIN_CAMERA_DEPTH.height) {
            MAIN_CAMERA_DEPTH.resize(mainRenderTarget.width, mainRenderTarget.height, Minecraft.ON_OSX);
        }
        if (mainRenderTarget.isStencilEnabled()) {
            MAIN_CAMERA_DEPTH.enableStencil();
        } else if (MAIN_CAMERA_DEPTH.isStencilEnabled()) {
            MAIN_CAMERA_DEPTH.destroyBuffers();
            MAIN_CAMERA_DEPTH = new DepthOnlyRenderTarget(mainRenderTarget.width, mainRenderTarget.height);
        }
        MAIN_CAMERA_DEPTH.copyDepthFrom(mainRenderTarget);
        mainRenderTarget.bindWrite(false);
    }

    /**
     * Renders the projected images of {@link ProjectorBlockEntity} instances that were registered via
     * {@link #addProjector(ProjectorBlockEntity, Vec3, float)} this frame.
     */
    @SubscribeEvent
    public static void renderProjectors(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (isIsRenderingProjectorDepth()) {
            return;
        }

        if (VISIBLE_PROJECTORS.isEmpty()) {
            return;
        }
        try {
            final Minecraft minecraft = Minecraft.getInstance();
            final ClientLevel level = minecraft.level;
            final LocalPlayer player = minecraft.player;
            if (level == null || player == null) {
                return;
            }

            VISIBLE_PROJECTORS.sort((p1, p2) -> {
                final double distance1 = player.distanceToSqr(p1.worldPos());
                final double distance2 = player.distanceToSqr(p2.worldPos());
                return Double.compare(distance1, distance2);
            });

            final int projectorCount = Math.min(VISIBLE_PROJECTORS.size(), ModShaders.MAX_PROJECTORS);
            renderProjectorDepths(minecraft, level, event.getPartialTick(), projectorCount);
            renderProjectorColors(minecraft, event.getPoseStack().last().pose(), event.getProjectionMatrix(), projectorCount);

        } finally {
            VISIBLE_PROJECTORS.clear();
            Arrays.fill(PROJECTOR_COLOR_TARGETS, null);
        }
    }

    /**
     * Suppresses fog rendering while rendering depth buffer for projectors.
     */
    @SubscribeEvent
    public static void handleFog(final ViewportEvent.RenderFog event) {
        if (isRenderingProjectorDepth) {
            FogRenderer.setupNoFog();
        }
    }

    /**
     * Suppresses nameplate rendering while rendering depth buffer for projectors.
     */
    @SubscribeEvent
    public static void handleNameplate(final RenderNameTagEvent event) {
        if (isRenderingProjectorDepth) {
            event.setCanRender(TriState.FALSE);
        }
    }

    /**
     * Updates cached rendering info, such as textures holding image data for projectors, to allow expiration.
     */
    @SubscribeEvent
    public static void handleClientTick(final ClientTickEvent.Pre event) {
        RENDER_INFO.cleanUp();
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Stage one of projector rendering, render scene depths from the perspective of all projectors that should
     * bre rendered. The output is the list of depth buffers, MVP matrices that were used to render them, and the
     * associated color texture for the projector.
     */
    private static void renderProjectorDepths(final Minecraft minecraft, final ClientLevel level,
                                              final DeltaTracker deltaTracker, final int projectorCount) {
        final Vec3 mainCameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        prepareDepthBufferRendering(minecraft, level, deltaTracker);
        try {
            final PoseStack viewModelStack = new PoseStack();
            for (int projectorIndex = 0; projectorIndex < projectorCount; projectorIndex++) {
                final VisibleProjector entry = VISIBLE_PROJECTORS.get(projectorIndex);
                final ProjectorBlockEntity projector = entry.projector();
                // Use the world-space position and Y-rotation computed by the
                // BER from its PoseStack. For a contraption block,
                // projector.getBlockPos() is a far-away virtual coordinate
                // and blockState.FACING.toYRot() doesn't account for the
                // contraption's rotation — both would point the depth camera
                // at the wrong place.
                final Vec3 projectorPos = entry.worldPos();
                final float yRot = entry.yRot();

                configureProjectorDepthCamera(level, projectorPos, yRot);

                RenderSystem.setProjectionMatrix(DEPTH_CAMERA_PROJECTION_MATRIX, VertexSorting.DISTANCE_TO_ORIGIN);
                setupViewModelMatrix(viewModelStack);

                storeProjectorMatrix(projectorIndex, projectorPos, mainCameraPosition, viewModelStack);

                bindProjectorDepthRenderTarget(projectorIndex, minecraft);

                renderProjectorDepthBuffer(minecraft, deltaTracker, viewModelStack);

                storeProjectorColorBuffer(projectorIndex, projector);

                projector.onRendering();
            }
        } finally {
            finishDepthBufferRendering(minecraft);
        }
    }

    private static void prepareDepthBufferRendering(final Minecraft minecraft, final ClientLevel level, final DeltaTracker deltaTracker) {
        isRenderingProjectorDepth = true;

        // Suppresses hit outlines being rendered.
        hitResultBak = minecraft.hitResult;
        minecraft.hitResult = null;

        // Skip shadow rendering for perf.
        entityShadowsBak = minecraft.options.entityShadows().get();
        minecraft.options.entityShadows().set(false);

        minecraftCameraEntityBak = minecraft.getCameraEntity();
        minecraft.setCameraEntity(ProjectorCameraEntity.get(level, Vec3.ZERO, deltaTracker.getRealtimeDeltaTicks()));

        RenderSystem.backupProjectionMatrix();
    }

    private static void finishDepthBufferRendering(final Minecraft minecraft) {
        minecraft.hitResult = hitResultBak;
        minecraft.options.entityShadows().set(entityShadowsBak);

        RenderSystem.restoreProjectionMatrix();

        ((MinecraftExt) minecraft).setMainRenderTargetOverride(null);
        minecraft.getMainRenderTarget().bindWrite(true);

        minecraft.setCameraEntity(minecraftCameraEntityBak);

        isRenderingProjectorDepth = false;
    }

    private static void configureProjectorDepthCamera(final ClientLevel level, final Vec3 pos, final float rotationY) {
        PROJECTOR_DEPTH_CAMERA.setup(level, ProjectorCameraEntity.get(level, pos, rotationY), false, false, 0);
    }

    private static void setupViewModelMatrix(final PoseStack viewModelStack) {
        viewModelStack.setIdentity();
        viewModelStack.mulPose(Axis.YP.rotationDegrees(PROJECTOR_DEPTH_CAMERA.getYRot() + 180));
    }

    private static void storeProjectorMatrix(final int projectorIndex, final Vec3 projectorPos, final Vec3 mainCameraPosition, final PoseStack viewModelStack) {
        // Save model-view-projection matrix for mapping in compositing shader. We use the position relative to the
        // main camera here, so that the main camera can sit at the origin. This avoids loss of precision.
        PROJECTOR_CAMERA_MATRICES[projectorIndex] = new Matrix4f(DEPTH_CAMERA_PROJECTION_MATRIX);
        viewModelStack.pushPose();
        viewModelStack.translate(
            mainCameraPosition.x() - projectorPos.x(),
            mainCameraPosition.y() - projectorPos.y(),
            mainCameraPosition.z() - projectorPos.z()
        );
        PROJECTOR_CAMERA_MATRICES[projectorIndex].mul(viewModelStack.last().pose());
        viewModelStack.popPose();
    }

    private static void bindProjectorDepthRenderTarget(final int projectorIndex, final Minecraft minecraft) {
        final DepthOnlyRenderTarget projectorDepthTarget = PROJECTOR_DEPTH_TARGETS[projectorIndex];
        projectorDepthTarget.bindWrite(true);
        ((MinecraftExt) minecraft).setMainRenderTargetOverride(projectorDepthTarget);
    }

    private static void renderProjectorDepthBuffer(final Minecraft minecraft, final DeltaTracker deltaTracker, final PoseStack viewModelStack) {
       final LevelRenderer levelRenderer = minecraft.levelRenderer;
        levelRenderer.prepareCullFrustum(
            PROJECTOR_DEPTH_CAMERA.getPosition(),
            viewModelStack.last().pose(),
            DEPTH_CAMERA_PROJECTION_MATRIX
        );
        levelRenderer.renderLevel(
            deltaTracker,
            /*shouldRenderBlockOutline*/  false,
            PROJECTOR_DEPTH_CAMERA,
            minecraft.gameRenderer,
            minecraft.gameRenderer.lightTexture(),
            viewModelStack.last().pose(),
            DEPTH_CAMERA_PROJECTION_MATRIX
        );
    }

    private static void storeProjectorColorBuffer(final int projectorIndex, final ProjectorBlockEntity projector) {
        PROJECTOR_COLOR_TARGETS[projectorIndex] = getColorBuffer(projector);
    }

    /**
     * Stage two or projector rendering, this composes the projections of the projectors being rendered into the
     * main render target, using the camera matrices and depth information to determine where to render. This is
     * essentially a post-processing pass, i.e. it renders a screen-filling rectangle blending the projector light
     * into the existing main render target output.
     */
    private static void renderProjectorColors(final Minecraft minecraft, final Matrix4f pose, final Matrix4f projectionMatrix, final int projectorCount) {
        var modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
        modelViewMatrix.mul(pose);

        prepareColorBufferRendering();
        try {
            prepareOrthographicRendering(minecraft);

            RenderSystem.setShader(ModShaders::getProjectorsShader);
            ModShaders.configureProjectorsShader(
                MAIN_CAMERA_DEPTH,
                constructInverseMainCameraMatrix(modelViewMatrix, projectionMatrix),
                PROJECTOR_COLOR_TARGETS,
                PROJECTOR_DEPTH_TARGETS,
                PROJECTOR_CAMERA_MATRICES,
                projectorCount
            );

            renderIntoScreenRect();
        } finally {
            finishColorBufferRendering();
        }
    }

    private static void prepareColorBufferRendering() {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.getModelViewStack().pushMatrix();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static void finishColorBufferRendering() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableBlend();

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void prepareOrthographicRendering(final Minecraft minecraft) {
        final Matrix4f screenProjectionMatrix = (new Matrix4f()).setOrtho(0f, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), 0, 1000, 3000);

        RenderSystem.setProjectionMatrix(screenProjectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);

        final Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.identity();
        modelViewStack.translate(0, 0, -2000);
        RenderSystem.applyModelViewMatrix();
    }

    private static Matrix4f constructInverseMainCameraMatrix(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix) {
        final Matrix4f inverseModelViewMatrix = new Matrix4f(projectionMatrix);
        inverseModelViewMatrix.mul(modelViewMatrix);
        inverseModelViewMatrix.invert();
        return inverseModelViewMatrix;
    }

    private static void renderIntoScreenRect() {
        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        builder.addVertex(0, 0, 0).setUv(0, 1);
        builder.addVertex(0, MAIN_CAMERA_DEPTH.height, 0).setUv(0, 0);
        builder.addVertex(MAIN_CAMERA_DEPTH.width, MAIN_CAMERA_DEPTH.height, 0).setUv(1, 0);
        builder.addVertex(MAIN_CAMERA_DEPTH.width, 0, 0).setUv(1, 1);

        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static DynamicTexture getColorBuffer(final ProjectorBlockEntity projector) {
        try {
            return RENDER_INFO.get(projector, () -> {
                final DynamicTexture texture = new DynamicTexture(ProjectorDevice.WIDTH, ProjectorDevice.HEIGHT, false);
                texture.upload();
                final ProjectorDepthRenderInfo renderInfo = new ProjectorDepthRenderInfo(texture);
                projector.setFrameConsumer(renderInfo);
                return renderInfo;
            }).texture();
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////

}
