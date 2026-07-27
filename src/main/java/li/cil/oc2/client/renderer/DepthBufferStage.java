package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;

import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import li.cil.oc2.common.ext.MinecraftExt;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

final class DepthBufferStage {
    private static final Camera PROJECTOR_DEPTH_CAMERA = new Camera();
    private static final float PROJECTOR_FORWARD_SHIFT = 7 / 16f;
    private static final float PROJECTOR_NEAR = 0.5f - PROJECTOR_FORWARD_SHIFT;
    private static final float PROJECTOR_FAR = ProjectorBlockEntity.MAX_RENDER_DISTANCE;
    private static final int FRUSTUM_WIDTH = (ProjectorBlockEntity.MAX_WIDTH - 1) / 2;
    private static final int FRUSTUM_HEIGHT = ProjectorBlockEntity.MAX_HEIGHT - 1;
    private static final Matrix4f DEPTH_CAMERA_PROJECTION_MATRIX =
            new Matrix4f()
                    .frustum(
                            -calculateFrustumComponent(FRUSTUM_WIDTH),
                            calculateFrustumComponent(FRUSTUM_WIDTH),
                            0,
                            calculateFrustumComponent(FRUSTUM_HEIGHT),
                            PROJECTOR_NEAR,
                            PROJECTOR_FAR);

    private static HitResult hitResultBak;
    private static boolean entityShadowsBak;
    private static Entity minecraftCameraEntityBak;

    private static float calculateFrustumComponent(final float originalValue) {
        return (originalValue / (ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE + 4f))
                / ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE;
    }

    static void renderProjectorDepths(
            final Minecraft minecraft,
            final ClientLevel level,
            final DeltaTracker deltaTracker,
            final int projectorCount) {
        final Vec3 mainCameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        prepareDepthBufferRendering(minecraft, level, deltaTracker);
        try {
            final PoseStack viewModelStack = new PoseStack();
            for (int projectorIndex = 0; projectorIndex < projectorCount; projectorIndex++) {
                final var entry = ProjectorDepthRenderer.VISIBLE_PROJECTORS.get(projectorIndex);
                final ProjectorBlockEntity projector = entry.projector();
                final Vec3 projectorPos = entry.worldPos();
                final float yRot = entry.yRot();

                configureProjectorDepthCamera(level, projectorPos, yRot);

                RenderSystem.setProjectionMatrix(
                        DEPTH_CAMERA_PROJECTION_MATRIX, VertexSorting.DISTANCE_TO_ORIGIN);
                setupViewModelMatrix(viewModelStack);

                storeProjectorMatrix(
                        projectorIndex, projectorPos, mainCameraPosition, viewModelStack);

                bindProjectorDepthRenderTarget(projectorIndex, minecraft);

                renderProjectorDepthBuffer(minecraft, deltaTracker, viewModelStack);

                ProjectorDepthRenderer.PROJECTOR_COLOR_TARGETS[projectorIndex] =
                        getColorBuffer(projector);

                projector.onRendering();
            }
        } finally {
            finishDepthBufferRendering(minecraft);
        }
    }

    private static void prepareDepthBufferRendering(
            final Minecraft minecraft, final ClientLevel level, final DeltaTracker deltaTracker) {
        ProjectorDepthRenderer.isRenderingProjectorDepth = true;

        hitResultBak = minecraft.hitResult;
        minecraft.hitResult = null;

        entityShadowsBak = minecraft.options.entityShadows().get();
        minecraft.options.entityShadows().set(false);

        minecraftCameraEntityBak = minecraft.getCameraEntity();
        minecraft.setCameraEntity(
                ProjectorCameraEntity.get(level, Vec3.ZERO, deltaTracker.getRealtimeDeltaTicks()));

        RenderSystem.backupProjectionMatrix();
    }

    private static void finishDepthBufferRendering(final Minecraft minecraft) {
        minecraft.hitResult = hitResultBak;
        minecraft.options.entityShadows().set(entityShadowsBak);

        RenderSystem.restoreProjectionMatrix();

        ((MinecraftExt) minecraft).setMainRenderTargetOverride(null);
        minecraft.getMainRenderTarget().bindWrite(true);

        minecraft.setCameraEntity(minecraftCameraEntityBak);

        ProjectorDepthRenderer.isRenderingProjectorDepth = false;
    }

    private static void configureProjectorDepthCamera(
            final ClientLevel level, final Vec3 pos, final float rotationY) {
        PROJECTOR_DEPTH_CAMERA.setup(
                level, ProjectorCameraEntity.get(level, pos, rotationY), false, false, 0);
    }

    private static void setupViewModelMatrix(final PoseStack viewModelStack) {
        viewModelStack.setIdentity();
        viewModelStack.mulPose(Axis.YP.rotationDegrees(PROJECTOR_DEPTH_CAMERA.getYRot() + 180));
    }

    private static void storeProjectorMatrix(
            final int projectorIndex,
            final Vec3 projectorPos,
            final Vec3 mainCameraPosition,
            final PoseStack viewModelStack) {
        ProjectorDepthRenderer.PROJECTOR_CAMERA_MATRICES[projectorIndex] =
                new Matrix4f(DEPTH_CAMERA_PROJECTION_MATRIX);
        viewModelStack.pushPose();
        viewModelStack.translate(
                mainCameraPosition.x() - projectorPos.x(),
                mainCameraPosition.y() - projectorPos.y(),
                mainCameraPosition.z() - projectorPos.z());
        ProjectorDepthRenderer.PROJECTOR_CAMERA_MATRICES[projectorIndex].mul(
                viewModelStack.last().pose());
        viewModelStack.popPose();
    }

    private static void bindProjectorDepthRenderTarget(
            final int projectorIndex, final Minecraft minecraft) {
        final var projectorDepthTarget =
                ProjectorDepthRenderer.PROJECTOR_DEPTH_TARGETS[projectorIndex];
        projectorDepthTarget.bindWrite(true);
        ((MinecraftExt) minecraft).setMainRenderTargetOverride(projectorDepthTarget);
    }

    private static void renderProjectorDepthBuffer(
            final Minecraft minecraft,
            final DeltaTracker deltaTracker,
            final PoseStack viewModelStack) {
        final LevelRenderer levelRenderer = minecraft.levelRenderer;
        levelRenderer.prepareCullFrustum(
                PROJECTOR_DEPTH_CAMERA.getPosition(),
                viewModelStack.last().pose(),
                DEPTH_CAMERA_PROJECTION_MATRIX);
        levelRenderer.renderLevel(
                deltaTracker,
                false,
                PROJECTOR_DEPTH_CAMERA,
                minecraft.gameRenderer,
                minecraft.gameRenderer.lightTexture(),
                viewModelStack.last().pose(),
                DEPTH_CAMERA_PROJECTION_MATRIX);
    }

    private static DynamicTexture getColorBuffer(final ProjectorBlockEntity projector) {
        return ProjectorDepthRenderer.getColorBuffer(projector);
    }
}
