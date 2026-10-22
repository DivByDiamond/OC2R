package li.cil.oc2.client.renderer.blockentity.monitor;

//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.client.renderer.blockentity.RendererQuadHelper;
import li.cil.oc2.client.renderer.stage.shader.ModRenderType;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.config.Config;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class MonitorTextRenderer {
    static void renderTerminal(
            final MonitorBlockEntity monitor,
            final PoseStack stack,
            final SubmitNodeCollector collector,
            final double distanceToCamera,
            final Font font,
            final int width,
            final int height) {
        stack.pushPose();

        // The multiblock screen surface is normalized by the renderer to always 16x16 units
        // (1 unit = width/16 blocks in X, height/16 blocks in Y). The monitor block has a
        // border/bezel of Config.monitorBorder px around the display area. Dividing border by
        // width/height keeps it a fixed pixel amount regardless of multiblock size. The terminal
        // framebuffer is stretched to fill this visible area edge-to-edge (non-uniform scale).

        final float border = Config.monitorBorder;
        final float borderX = border / width;
        final float borderY = border / height;
        final float visibleX = 16f - 2f * borderX;
        final float visibleY = 16f - 2f * borderY;

        final MonitorGUIRenderer terminal = (MonitorGUIRenderer) monitor.getMonitor();

        // Resolution of the last frame received from the server (falls back to the legacy
        // default before the first frame); keeps scale and quad in sync with the texture.
        final int framebufferWidth = monitor.video.getClientFrameWidth();
        final int framebufferHeight = monitor.video.getClientFrameHeight();

        final float scaleX = visibleX / framebufferWidth;
        final float scaleY = visibleY / framebufferHeight;
        MonitorScreenGeometry.translateToSurface(stack, borderX, borderY);
        stack.scale(scaleX, scaleY, 1f);

        final ResourceLocation texture = MonitorRendererCache.getRendererView(terminal, monitor).getTexture();
        if (texture != null) {
            collector.submitCustomGeometry(
                    stack,
                    ModRenderType.getFramebuffer(texture),
                    (pose, buffer) -> {
                        buffer.addVertex(pose, 0, 0, 0).setUv(0, 0);
                        buffer.addVertex(pose, 0, framebufferHeight, 0).setUv(0, 1);
                        buffer.addVertex(pose, framebufferWidth, framebufferHeight, 0).setUv(1, 1);
                        buffer.addVertex(pose, framebufferWidth, 0, 0).setUv(1, 0);
                    });
        }

        stack.popPose();
    }

    static void renderStatusText(
            final SubmitNodeCollector collector,
            final MonitorBlockEntity monitor,
            final PoseStack stack,
            final double distanceToCamera,
            final Font font,
            final int width) {
        if (distanceToCamera > 12f) {
            return;
        }

        final Component bootError =
                Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY);

        stack.pushPose();
        // Coordinates here are normalized so the multiblock screen is always 16x16 units.
        // drawText centers the line horizontally (X is already at the screen center); nudge it
        // down to the vertical center so it doesn't hug the top edge.
        MonitorScreenGeometry.translateToSurface(stack, 3, 7.5f);

        RendererQuadHelper.drawText(collector, stack, bootError, font);

        stack.popPose();
    }

    static void cleanUp() {
        MonitorRendererCache.cleanUp();
        MonitorGUIRenderer.cleanUp();
    }
}
*///?} else {
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.renderer.MonitorGUIRenderer;
import li.cil.oc2.client.renderer.blockentity.RendererQuadHelper;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.monitor.MonitorBlockEntity;
import li.cil.oc2.common.config.Config;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

final class MonitorTextRenderer {
    static void renderTerminal(
            final MonitorBlockEntity monitor,
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final double distanceToCamera,
            final Font font,
            final int width,
            final int height) {
        stack.pushPose();

        // The multiblock screen surface is normalized by the renderer to always 16x16 units
        // (1 unit = width/16 blocks in X, height/16 blocks in Y). The monitor block has a
        // border/bezel of Config.monitorBorder px around the display area. Dividing border by
        // width/height keeps it a fixed pixel amount regardless of multiblock size. The terminal
        // framebuffer is stretched to fill this visible area edge-to-edge (non-uniform scale).

        final float border = Config.monitorBorder;
        final float borderX = border / width;
        final float borderY = border / height;
        final float visibleX = 16f - 2f * borderX;
        final float visibleY = 16f - 2f * borderY;

        final MonitorGUIRenderer terminal = (MonitorGUIRenderer) monitor.getMonitor();

        // Resolution of the last frame received from the server (falls back to the legacy
        // default before the first frame); keeps scale and quad in sync with the texture.
        final int framebufferWidth = monitor.video.getClientFrameWidth();
        final int framebufferHeight = monitor.video.getClientFrameHeight();

        final float scaleX = visibleX / framebufferWidth;
        final float scaleY = visibleY / framebufferHeight;
        MonitorScreenGeometry.translateToSurface(stack, borderX, borderY);
        stack.scale(scaleX, scaleY, 1f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        MonitorRendererCache.getRendererView(terminal, monitor)
                .render(
                        stack,
                        RenderSystem.getProjectionMatrix(),
                        framebufferWidth,
                        framebufferHeight,
                        true);

        stack.popPose();
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    static void renderStatusText(
            final MultiBufferSource bufferSource,
            final MonitorBlockEntity monitor,
            final PoseStack stack,
            final double distanceToCamera,
            final Font font,
            final int width) {
        if (distanceToCamera > 12f) {
            return;
        }

        final Component bootError =
                Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY);

        stack.pushPose();
        // Coordinates here are normalized so the multiblock screen is always 16x16 units.
        // drawText centers the line horizontally (X is already at the screen center); nudge it
        // down to the vertical center so it doesn't hug the top edge.
        MonitorScreenGeometry.translateToSurface(stack, 3, 7.5f);

        RendererQuadHelper.drawText(bufferSource, stack, bootError, font);

        stack.popPose();
    }

    static void cleanUp() {
        MonitorRendererCache.cleanUp();
        MonitorGUIRenderer.cleanUp();
    }
}
//?}
