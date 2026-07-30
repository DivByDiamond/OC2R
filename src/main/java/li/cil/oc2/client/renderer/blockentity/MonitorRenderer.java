package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.monitor.MonitorBlock;
import li.cil.oc2.common.block.monitor.MonitorMultiblock;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.blockentity.monitor.MonitorContraptionHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;

@SuppressWarnings("unused")
@EventBusSubscriber(value = Dist.CLIENT, modid = API.MOD_ID)
public final class MonitorRenderer implements BlockEntityRenderer<MonitorBlockEntity> {
    private final Font font;

    public MonitorRenderer(final BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(
            final MonitorBlockEntity monitor,
            final float partialTicks,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final int light,
            final int overlay) {
        // Only the origin (master) of a multiblock renders the framebuffer. Sub-blocks are
        // just casing — their visuals come entirely from the baked block model. This also
        // prevents the framebuffer from being drawn N times across an N-block multiblock.
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
                blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
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

        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(blockFacing.toYRot() + 180));
        stack.translate(-0.5f, 0, -0.5f);

        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale * width, pixelScale * height, pixelScale);

        if (framebufferSource.getPowerState()
                && framebufferSource.isMounted()
                && framebufferSource.hasPower()) {
            MonitorTextRenderer.renderTerminal(
                    framebufferSource, stack, bufferSource, distanceToCamera, font);
        } else if (framebufferSource.getPowerState()) {
            MonitorTextRenderer.renderStatusText(
                    bufferSource, framebufferSource, stack, distanceToCamera, font);
        }

        stack.popPose();

        // ---- Power overlay -----------------------------------------------------
        // Drawn at the origin block's corner (which is the top-right of the multiblock from
        // the viewer's POV) at the original 1-block scale so the icon keeps its aspect ratio.
        if (framebufferSource.getPowerState() && framebufferSource.hasPower()) {
            stack.pushPose();

            stack.translate(0.5f, 0, 0.5f);
            stack.mulPose(Axis.YN.rotationDegrees(blockFacing.toYRot() + 180));
            stack.translate(-0.5f, 0, -0.5f);

            stack.translate(1, 1, 0);
            stack.scale(-1, -1, -1);

            stack.scale(pixelScale, pixelScale, pixelScale);
            stack.translate(0, 0, -0.1f);

            final Matrix4f matrix = stack.last().pose();
            MonitorTextRenderer.renderPowerOverlay(matrix, bufferSource);

            stack.popPose();
        }
    }

    @SubscribeEvent
    public static void updateCache(final ClientTickEvent.Pre event) {
        MonitorTextRenderer.cleanUp();
    }
}
