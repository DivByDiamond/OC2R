package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.MonitorBlock;
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
    public void render(final MonitorBlockEntity monitor, final float partialTicks, final PoseStack stack, final MultiBufferSource bufferSource, final int light, final int overlay) {
        final MonitorBlockEntity framebufferSource = MonitorContraptionHelper.getPrimaryForContraptionRendering(monitor);

        final Direction blockFacing = monitor.getBlockState().getValue(MonitorBlock.FACING);

        final Matrix4f poseMatrix = stack.last().pose();
        final Vec3 blockWorldPosRelativeToCamera = new Vec3(poseMatrix.m30(), poseMatrix.m31(), poseMatrix.m32());
        final Vec3 blockCenterRelativeToCamera = blockWorldPosRelativeToCamera.add(0.5, 0.5, 0.5);

        final Vec3 blockCenterToCamera = blockCenterRelativeToCamera.scale(-1);
        final double projectedCameraPosition = blockCenterToCamera.dot(Vec3.atLowerCornerOf(blockFacing.getNormal()));
        if (!MonitorContraptionHelper.isContraptionVirtualClone(monitor) && projectedCameraPosition <= 0) {
            return;
        }

        final double distanceToCamera = blockCenterRelativeToCamera.length();

        stack.pushPose();

        stack.translate(0.5f, 0, 0.5f);
        stack.mulPose(Axis.YN.rotationDegrees(blockFacing.toYRot() + 180));
        stack.translate(-0.5f, 0, -0.5f);

        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        if (framebufferSource.getPowerState() && framebufferSource.isMounted() && framebufferSource.hasPower()) {
            MonitorTextRenderer.renderTerminal(framebufferSource, stack, bufferSource, distanceToCamera, font);
        } else if (framebufferSource.getPowerState()) {
            MonitorTextRenderer.renderStatusText(bufferSource, framebufferSource, stack, distanceToCamera, font);
        }

        stack.translate(0, 0, -0.1f);
        final Matrix4f matrix = stack.last().pose();

        if (framebufferSource.getPowerState() && framebufferSource.hasPower())
            renderPower(matrix, bufferSource);

        stack.popPose();
    }

    private void renderPower(final Matrix4f matrix, final MultiBufferSource bufferSource) {
        MonitorTextRenderer.renderPowerOverlay(matrix, bufferSource);
    }

    @SubscribeEvent
    public static void updateCache(final ClientTickEvent.Pre event) {
        MonitorTextRenderer.cleanUp();
    }
}
