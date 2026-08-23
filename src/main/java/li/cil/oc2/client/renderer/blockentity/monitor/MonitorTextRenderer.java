package li.cil.oc2.client.renderer.blockentity.monitor;

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
        //
        // Origin correction: the origin block sits at the top-right of the multiblock, but the
        // renderer's R(Y,180) flips X (not Y), so the X component of translate(1,1,0) lands at
        // the LEFT edge of the origin block (x~0) instead of the RIGHT edge of the multiblock
        // (x~1). This leaves the screen shifted right by (width-1) blocks. Shift it back
        // leftward, auto-computed from width so no manual offset is needed for any size.
        final float originShiftX = -16f * (width - 1) / width;
        stack.translate(originShiftX, 0f, 0f);

        final float border = Config.monitorBorder;
        final float borderX = border / width;
        final float borderY = border / height;
        final float visibleX = 16f - 2f * borderX;
        final float visibleY = 16f - 2f * borderY;

        final MonitorGUIRenderer terminal = monitor.getMonitor();

        // Resolution of the last frame received from the server (falls back to the legacy
        // default before the first frame); keeps scale and quad in sync with the texture.
        final int framebufferWidth = monitor.video.getClientFrameWidth();
        final int framebufferHeight = monitor.video.getClientFrameHeight();

        final float scaleX = visibleX / framebufferWidth;
        final float scaleY = visibleY / framebufferHeight;
        stack.translate(borderX, borderY, 0.7f);
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
        // Same origin correction as renderTerminal: R(Y,180) flips X, shifting the screen right
        // by (width-1) blocks. Shift it back so the text lands in the visible area.
        final float originShiftX = -16f * (width - 1) / width;
        stack.translate(originShiftX, 0f, 0f);
        // Coordinates here are normalized so the multiblock screen is always 16x16 units.
        // drawText centers the line horizontally (X is already at the screen center); nudge it
        // down to the vertical center so it doesn't hug the top edge.
        stack.translate(3, 7.5f, 0.7f);

        RendererQuadHelper.drawText(bufferSource, stack, bootError, font);

        stack.popPose();
    }

    static void cleanUp() {
        MonitorRendererCache.cleanUp();
    }
}
