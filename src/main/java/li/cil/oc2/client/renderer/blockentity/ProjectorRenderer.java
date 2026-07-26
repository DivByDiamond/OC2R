package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.blockentity.projector.ProjectorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public final class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {
    private static final float PROJECTOR_FORWARD_SHIFT = 7 / 16f;

    public ProjectorRenderer(final BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public boolean shouldRender(final ProjectorBlockEntity projector, final Vec3 position) {
        return !ProjectorDepthRenderer.isIsRenderingProjectorDepth() &&
            BlockEntityRenderer.super.shouldRender(projector, position);
    }

    @Override
    public boolean shouldRenderOffScreen(final ProjectorBlockEntity projector) {
        return true;
    }

    @Override
    public void render(final ProjectorBlockEntity projector, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        final ProjectorBlockEntity framebufferSource = projector.getPrimaryForContraptionRendering();

        if (!framebufferSource.isProjecting()) {
            return;
        }

        final org.joml.Matrix4f preAlignMatrix = new org.joml.Matrix4f(stack.last().pose());

        stack.pushPose();
        alignToFrontFace(projector, stack);

        if (!framebufferSource.hasEnergy()) {
            ProjectorLightRenderer.renderMissingEnergyIndicator(stack, bufferSource);
            stack.popPose();
            return;
        }

        final net.minecraft.client.Camera camera =
            net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();

        final org.joml.Vector4f blockCenterCam = new org.joml.Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
        blockCenterCam.mul(preAlignMatrix);
        final Vec3 blockCenterWorld = camera.getPosition().add(
            blockCenterCam.x(), blockCenterCam.y(), blockCenterCam.z());

        final org.joml.Matrix4f postAlignMatrix = stack.last().pose();
        final org.joml.Vector4f lightDirCam = new org.joml.Vector4f(0, 0, 1, 0);
        lightDirCam.mul(postAlignMatrix);
        final Vec3 lightDirWorld = new Vec3(lightDirCam.x(), lightDirCam.y(), lightDirCam.z())
            .normalize();

        final Vec3 projectorPos = blockCenterWorld.add(lightDirWorld.scale(PROJECTOR_FORWARD_SHIFT));

        final float yRot = (float) Math.toDegrees(Math.atan2(-lightDirWorld.x, lightDirWorld.z));

        if (canSeeProjectedImage(stack)) {
            ProjectorDepthRenderer.addProjector(framebufferSource, projectorPos, yRot);
        }

        ProjectorLightRenderer.renderProjectorLight(stack, bufferSource);

        stack.popPose();
    }

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

    @Override
    public AABB getRenderBoundingBox(final ProjectorBlockEntity block) {
        return block.getRenderBoundingBox();
    }
}
