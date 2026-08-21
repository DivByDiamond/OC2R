package li.cil.oc2.client.renderer.blockentity.computer;

import com.google.common.cache.Cache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.concurrent.ExecutionException;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.render.RendererView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

final class TerminalTextRenderer {
    private final Font font;
    private final Cache<Terminal, RendererView> rendererViews;

    TerminalTextRenderer(
            final Font font, final Cache<Terminal, RendererView> rendererViews) {
        this.font = font;
        this.rendererViews = rendererViews;
    }

    void renderTerminal(
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final Terminal terminal,
            final double distanceToCamera) {
        // Render terminal content if close enough.
        if (distanceToCamera >= 6f) {
            return;
        }

        stack.pushPose();
        stack.translate(2, 2, -0.9f);

        final float textScaleX = 12f / terminal.getWidth();
        final float textScaleY = 7f / terminal.getHeight();
        // Keep aspect ratio by using independent X/Y scales. At 80 cols both produce
        // the same uniform scale. At 132 cols X shrinks (narrower chars) while Y
        // stays the same — characters squish horizontally like a real VT100.
        final float scaleX = textScaleX * 0.95f;
        final float scaleY = textScaleY * 0.95f;

        // Center the terminal in the block face.
        stack.translate(
                terminal.getWidth() * (textScaleX - scaleX) * 0.5f,
                terminal.getHeight() * (textScaleY - scaleY) * 0.5f,
                0f);

        stack.scale(scaleX, scaleY, 1f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        try {
            rendererViews.get(terminal, terminal::getRenderer)
                    .render(stack, RenderSystem.getProjectionMatrix(), true);
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }

        stack.popPose();
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    void renderStatusText(
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final double distanceToCamera,
            final Component bootError) {
        if (distanceToCamera > 12f) {
            return;
        }

        if (bootError == null) {
            return;
        }

        stack.pushPose();
        stack.translate(3, 3, -0.9f);

        drawText(bufferSource, stack, bootError);

        stack.popPose();
    }

    private void drawText(
            final MultiBufferSource bufferSource, final PoseStack stack, final Component text) {
        final int maxWidth = 100;

        stack.pushPose();
        stack.scale(10f / maxWidth, 10f / maxWidth, 10f / maxWidth);

        final List<FormattedText> wrappedText =
                font.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = font.width(text);
            draw(bufferSource, font, stack, text, (maxWidth - textWidth) * 0.5f);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                draw(
                        bufferSource,
                        font,
                        stack,
                        wrappedText.get(i).getString(),
                        i * font.lineHeight);
            }
        }

        stack.popPose();
    }

    private void draw(
            final MultiBufferSource bufferSource,
            final Font font,
            final PoseStack stack,
            final Component text,
            final float x) {
        font.drawInBatch(
                text,
                x,
                (float) 0,
                15610658,
                false,
                stack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880);
    }

    private void draw(
            final MultiBufferSource bufferSource,
            final Font font,
            final PoseStack stack,
            final String text,
            final float y) {
        font.drawInBatch(
                text,
                (float) 0,
                y,
                15610658,
                false,
                stack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880,
                false);
    }
}
