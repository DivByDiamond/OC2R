/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc2.common.blockentity.InternetGateWayBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;

public final class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {
    private static final int LIGHT_COLOR_NEAR = 0x22FFFFFF;
    private static final int LIGHT_COLOR_FAR = 0x00FFFFFF;
    private static final int LENS_COLOR = 0xDDFFFFFF;
    private static final int MISSING_ENERGY_COLOR = 0xEEFF6666;
    private static final int LED_COLOR = 0xCC6688DD;

    private static final float LENS_RIGHT = 0 + 4 / 16f;
    private static final float LENS_LEFT = 1 - 4 / 16f;
    private static final float LENS_BOTTOM = 0 + 4 / 16f;
    private static final float LENS_TOP = 1 - 4 / 16f;

    /** Forward offset from the projector block's center, matches ProjectorDepthRenderer. */
    private static final float PROJECTOR_FORWARD_SHIFT = 7 / 16f;

    ///////////////////////////////////////////////////////////////////

    public ProjectorRenderer(final BlockEntityRendererProvider.Context ignored) {
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean shouldRender(final ProjectorBlockEntity projector, final Vec3 position) {
        return !ProjectorDepthRenderer.isIsRenderingProjectorDepth() &&
            BlockEntityRenderer.super.shouldRender(projector, position);
    }

    @Override
    public boolean shouldRenderOffScreen(final ProjectorBlockEntity projector) {
        // Render bounding box of projectors (vastly) exceeds their block position, so they need
        // to be treated as global renderers, and cannot be culled with their chunk.
        return true;
    }

    @Override
    public void render(final ProjectorBlockEntity projector, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        // When rendering a Sable / Create: Aeronautics contraption, the
        // BlockEntity passed to us is a "virtual clone" at a far-away virtual
        // BlockPos. The clone's framebuffer never receives updates and its
        // raw BlockPos is unsuitable for depth-camera setup. Look up the
        // primary projector (same persistent device id, real BlockPos) and
        // use its framebuffer for the projected image. Falls back to the
        // passed-in BlockEntity when not on a contraption.
        final ProjectorBlockEntity framebufferSource = projector.getPrimaryForContraptionRendering();

        if (!framebufferSource.isProjecting()) {
            return;
        }

        // Snapshot the PoseStack matrix BEFORE alignToFrontFace. This matrix
        // already contains the contraption transform (translation + rotation)
        // applied by Sable / Create: Aeronautics. We use it to derive the
        // projector's actual world position; the raw BlockPos may be a
        // far-away virtual coordinate on contraptions.
        //
        // We can't just read m30/m31/m32 + 0.5 because for a contraption the
        // matrix has a rotation component, and (0.5, 0.5, 0.5) in BE-local
        // coords does NOT map to (m30+0.5, m31+0.5, m32+0.5). We need the
        // full v * M transformation.
        final org.joml.Matrix4f preAlignMatrix = new org.joml.Matrix4f(stack.last().pose());

        stack.pushPose();
        alignToFrontFace(projector, stack);

        if (!framebufferSource.hasEnergy()) {
            renderMissingEnergyIndicator(stack, bufferSource);
            stack.popPose();
            return;
        }

        // Compute the projector's world-space center and forward direction
        // using the full PoseStack transform so contraption rotation is
        // correctly accounted for.
        //
        // Block center in BE-local coords = (0.5, 0.5, 0.5).
        // Projector light direction in BE-local coords AFTER alignToFrontFace
        // (which rotates the model so the lens points along blockFacing) =
        // (0, 0, 1) — i.e. the +Z axis of the post-align local frame is the
        // direction the projector emits light.
        final net.minecraft.client.Camera camera =
            net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();

        // Block center in camera space, then convert to world space.
        final org.joml.Vector4f blockCenterCam = new org.joml.Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
        blockCenterCam.mul(preAlignMatrix);
        final Vec3 blockCenterWorld = camera.getPosition().add(
            blockCenterCam.x(), blockCenterCam.y(), blockCenterCam.z());

        // Projector light direction in camera space (= world space direction,
        // since camera translation doesn't affect direction vectors).
        final org.joml.Matrix4f postAlignMatrix = stack.last().pose();
        final org.joml.Vector4f lightDirCam = new org.joml.Vector4f(0, 0, 1, 0);
        lightDirCam.mul(postAlignMatrix);
        final Vec3 lightDirWorld = new Vec3(lightDirCam.x(), lightDirCam.y(), lightDirCam.z())
            .normalize();

        // Projector depth-camera position = block center + forward shift
        // along the actual world-space light direction. This matches the
        // PROJECTOR_FORWARD_SHIFT baked into the projector model geometry.
        final Vec3 projectorPos = blockCenterWorld.add(lightDirWorld.scale(PROJECTOR_FORWARD_SHIFT));

        // Y-rotation matching MC's convention (0=south, 90=west, 180=north,
        // 270=east). Computed from the world-space light direction so that
        // contraption rotation is correctly accounted for. Using
        // blockState.getValue(FACING).toYRot() would ignore the contraption
        // rotation and point the depth camera in the wrong direction — the
        // previous bug made contraption projectors only render on the
        // southern wall of geometry around them, regardless of which way
        // the projector was physically aimed.
        final float yRot = (float) Math.toDegrees(Math.atan2(-lightDirWorld.x, lightDirWorld.z));

        if (canSeeProjectedImage(stack)) {
            ProjectorDepthRenderer.addProjector(framebufferSource, projectorPos, yRot);
        }

        renderProjectorLight(stack, bufferSource);

        stack.popPose();
    }

    ///////////////////////////////////////////////////////////////////

    private static boolean canSeeProjectedImage(final PoseStack stack) {
        final Matrix4f matrix = stack.last().pose();

        final Vector4f lookDirection = new Vector4f(0, 0, -1, 0);
        lookDirection.mul(matrix);

        final Vector4f relativePosition = new Vector4f(0, 0, 1, 1);
        relativePosition.mul(matrix);

        return relativePosition.dot(lookDirection) < ProjectorBlockEntity.MAX_RENDER_DISTANCE;
    }

    private void alignToFrontFace(final ProjectorBlockEntity projector, final PoseStack stack) {
        final Direction blockFacing = projector.getBlockState().getValue(ProjectorBlock.FACING);
        final Quaternionf rotation = Axis.YN.rotationDegrees(blockFacing.toYRot());
        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(rotation);
    }

    private static void renderProjectorLight(final PoseStack stack, final MultiBufferSource bufferSource) {
        stack.translate(-0.5, 0, 0.5);
        final VertexConsumer consumer = bufferSource.getBuffer(ModRenderType.getProjectorLight());
        final Matrix4f matrix = stack.last().pose();

        final float leftFar = 1.25f;
        final float rightFar = -0.25f;
        final float topFar = 1.5f;
        final float bottomFar = 0 + 1 / 16f;

        // Top.
        consumer.addVertex(matrix, leftFar, topFar, 1).setColor(LIGHT_COLOR_FAR); // top left far
        consumer.addVertex(matrix, LENS_LEFT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR); // top left near
        consumer.addVertex(matrix, LENS_RIGHT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR); // top right near
        consumer.addVertex(matrix, rightFar, topFar, 1).setColor(LIGHT_COLOR_FAR); // top right far

        // Bottom.
        consumer.addVertex(matrix, leftFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR); // bottom left far
        consumer.addVertex(matrix, LENS_LEFT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR); // bottom left near
        consumer.addVertex(matrix, LENS_RIGHT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR); // bottom right near
        consumer.addVertex(matrix, rightFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR); // bottom right far

        // Left.
        consumer.addVertex(matrix, leftFar, topFar, 1).setColor(LIGHT_COLOR_FAR); // top left far
        consumer.addVertex(matrix, leftFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR); // bottom left far
        consumer.addVertex(matrix, LENS_LEFT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR); // bottom left near
        consumer.addVertex(matrix, LENS_LEFT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR); // top left near

        // Right.
        consumer.addVertex(matrix, rightFar, topFar, 1).setColor(LIGHT_COLOR_FAR); // top right far
        consumer.addVertex(matrix, LENS_RIGHT, LENS_TOP, 0).setColor(LIGHT_COLOR_NEAR); // top right near
        consumer.addVertex(matrix, LENS_RIGHT, LENS_BOTTOM, 0).setColor(LIGHT_COLOR_NEAR); // bottom right near
        consumer.addVertex(matrix, rightFar, bottomFar, 1).setColor(LIGHT_COLOR_FAR); // bottom right far

        renderLens(matrix, consumer, LENS_COLOR);
        renderLed(matrix, consumer);
    }

    private static void renderMissingEnergyIndicator(final PoseStack stack, final MultiBufferSource bufferSource) {
        stack.translate(-0.5, 0, 0.5);
        final VertexConsumer consumer = bufferSource.getBuffer(ModRenderType.getProjectorLight());
        final Matrix4f matrix = stack.last().pose();

        renderLens(matrix, consumer, MISSING_ENERGY_COLOR);
        renderLed(matrix, consumer);
    }

    private static void renderLens(final Matrix4f matrix, final VertexConsumer consumer, final int color) {
        final float lensDepth = -1 / 16f;
        consumer.addVertex(matrix, LENS_RIGHT, LENS_BOTTOM, lensDepth).setColor(color);
        consumer.addVertex(matrix, LENS_LEFT, LENS_BOTTOM, lensDepth).setColor(color);
        consumer.addVertex(matrix, LENS_LEFT, LENS_TOP, lensDepth).setColor(color);
        consumer.addVertex(matrix, LENS_RIGHT, LENS_TOP, lensDepth).setColor(color);
    }

    private static void renderLed(final Matrix4f matrix, final VertexConsumer consumer) {
        final float ledRight = 0 + 7 / 16f;
        final float ledLeft = 0 + 9 / 16f;
        final float ledBottom = 0 + 3 / 16f;
        final float ledTop = 0 + 4 / 16f;
        final float ledDepth = -0.75f / 16f;

        consumer.addVertex(matrix, ledRight, ledBottom, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(matrix, ledLeft, ledBottom, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(matrix, ledLeft, ledTop, ledDepth).setColor(LED_COLOR);
        consumer.addVertex(matrix, ledRight, ledTop, ledDepth).setColor(LED_COLOR);
    }

    @Override
    public AABB getRenderBoundingBox(ProjectorBlockEntity block) {
        return block.getRenderBoundingBox();
    }
}
