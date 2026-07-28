package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import li.cil.oc2.common.vm.terminal.Terminal;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

final class TerminalTextRenderer {
    private final Font font;
    private final TerminalTextureRenderer textureRenderer;

    TerminalTextRenderer(final Font font) {
        this.font = font;
        this.textureRenderer = new TerminalTextureRenderer();
    }

    void renderTerminal(
            final PoseStack stack,
            final MultiBufferSource bufferSource,
            final Terminal terminal,
            final double distanceToCamera) {
        // Always use TerminalTextureRenderer for all distances (LOD: same texture,
        // just smaller via perspective projection).  NEAREST filtering keeps text crisp.
        stack.pushPose();
        stack.translate(2, 2, -0.9f);

        final float textScaleX = 16f / terminal.getWidth();
        final float textScaleY = 9f / terminal.getHeight();
        final float scale = Math.min(textScaleX, textScaleY) * 0.95f;

        final float scaleDeltaX = textScaleX - scale;
        final float scaleDeltaY = textScaleY - scale;
        stack.translate(
                terminal.getWidth() * scaleDeltaX * 0.5f,
                terminal.getHeight() * scaleDeltaY * 0.5f,
                0f);

        stack.scale(scale, scale, 1f);

        textureRenderer.render(stack, RenderSystem.getProjectionMatrix(), terminal, true);

        stack.popPose();
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