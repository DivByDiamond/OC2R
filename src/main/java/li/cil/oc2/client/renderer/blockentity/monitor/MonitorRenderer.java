package li.cil.oc2.client.renderer.blockentity.monitor;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=26.1 {
/*import javax.annotation.Nullable;
*///?}
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.block.monitor.MonitorMultiblock;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.monitor.misc.MonitorContraptionHelper;
import li.cil.oc2.platform.event.ClientEvents;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//? if >=26.1 {
/*import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
*///?}
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

@SuppressWarnings("unused")
//? if >=26.1 {
/*public final class MonitorRenderer
        implements BlockEntityRenderer<MonitorBlockEntity, MonitorRenderer.State> {
*///?} else {
public final class MonitorRenderer implements BlockEntityRenderer<MonitorBlockEntity> {
//?}
    private final Font font;

    public MonitorRenderer(final BlockEntityRendererProvider.Context context) {
        //? if >=26.1 {
/*        this.font = context.font();
*///?} else {
        this.font = context.getFont();
        //?}
    }

    //? if >=26.1 {
/*    public static final class State extends BlockEntityRenderState {
        MonitorBlockEntity monitor;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            final MonitorBlockEntity monitor,
            final State renderState,
            final float partialTicks,
            final Vec3 cameraPosition,
            @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                monitor, renderState, partialTicks, cameraPosition, breakProgress);
        renderState.monitor = monitor;
    }

    @Override
    public void submit(
            final State renderState,
            final PoseStack stack,
            final SubmitNodeCollector bufferSource,
            final CameraRenderState camera) {
        final MonitorBlockEntity monitor = renderState.monitor;
*///?} else {
    @Override
    public void render(
            final MonitorBlockEntity monitor,
            final float partialTicks,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final int light,
            final int overlay) {
    //?}
        if (!MonitorMultiblock.isOrigin(monitor.getBlockState())) {
            return;
        }

        final MonitorBlockEntity framebufferSource =
                MonitorContraptionHelper.getPrimaryForContraptionRendering(monitor);

        final BlockState state = monitor.getBlockState();
        final Direction blockFacing = state.getValue(MonitorBlock.FACING);
        final int width = state.getValue(MonitorBlock.WIDTH);
        final int height = state.getValue(MonitorBlock.HEIGHT);

        final Matrix4f poseMatrix = stack.last().pose();
        final Vec3 blockWorldPosRelativeToCamera =
                new Vec3(poseMatrix.m30(), poseMatrix.m31(), poseMatrix.m32());
        final Vec3 blockCenterRelativeToCamera = blockWorldPosRelativeToCamera.add(0.5, 0.5, 0.5);

        final Vec3 blockCenterToCamera = blockCenterRelativeToCamera.scale(-1);
        final double projectedCameraPosition =
                //? if >=26.1 {
/*                blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getUnitVec3i()));
*///?} else {
                blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
                //?}
        if (!MonitorContraptionHelper.isContraptionVirtualClone(monitor)
                && projectedCameraPosition <= 0) {
            return;
        }

        final double distanceToCamera = blockCenterRelativeToCamera.length();

        // ---- Framebuffer plane -------------------------------------------------
        // The origin block sits at the top-right corner of the multiblock from the viewer's
        // point of view. The existing (1, 1, 0) + scale(-1, -1, -1) transform places the
        // rendering origin at the top-right corner of the screen surface, extending left
        // (viewer POV) and down — exactly what we need for a multiblock whose origin is at
        // the top-right. Scaling by (width, height) in the X/Y pixel grid stretches the
        // framebuffer across the full W x H area.
        stack.pushPose();

        MonitorScreenGeometry.orient(stack, blockFacing, width, height);

        renderTerminalOrStatus(
                framebufferSource, stack, bufferSource, distanceToCamera, width, height);

        stack.popPose();
    }

    private void renderTerminalOrStatus(
            final MonitorBlockEntity framebufferSource,
            final PoseStack stack,
            //? if >=26.1 {
/*            final SubmitNodeCollector bufferSource,
*///?} else {
            final MultiBufferSource bufferSource,
            //?}
            final double distanceToCamera,
            final int width,
            final int height) {
        if (framebufferSource.getPowerState()
                && framebufferSource.isMounted()
                && framebufferSource.hasPower()) {
            MonitorTextRenderer.renderTerminal(
                    framebufferSource, stack, bufferSource, distanceToCamera, font, width, height);
        } else if (framebufferSource.getPowerState()) {
            MonitorTextRenderer.renderStatusText(
                    bufferSource, framebufferSource, stack, distanceToCamera, font, width);
        }
    }

    public static void registerEvents() {
        ClientEvents.CLIENT_TICK_START.register(() -> {
            MonitorTextRenderer.cleanUp();
        });
    }
}
