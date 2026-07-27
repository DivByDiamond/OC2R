package li.cil.oc2.common.vm.terminal;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
class TerminalCursorRenderer {
    static void renderCursor(final Terminal terminal, final PoseStack stack) {
        BufferUploader.reset();
        if (!terminal.currentPrivateModeState.DECTCEM) return;

        int globalY = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - terminal.y);
        int localY = Terminal.HEIGHT + (globalY - terminal.lastRowToDisplay);
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        if (terminal.x < 0
                || terminal.x >= Terminal.WIDTH
                || ((!useAltBuffer && localY < 0) || terminal.y < 0)
                || ((!useAltBuffer && localY >= Terminal.HEIGHT) || terminal.y >= Terminal.HEIGHT)
                || (!useAltBuffer && globalY > terminal.lastRowToDisplay)) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        stack.pushPose();
        stack.translate(
                terminal.x * Terminal.CHAR_WIDTH,
                (useAltBuffer ? terminal.y : localY) * Terminal.CHAR_HEIGHT,
                0);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(stack.last().pose());
        RenderSystem.applyModelViewMatrix();

        final Matrix4f matrix = new Matrix4f();
        final BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        final int foreground = TerminalColors.COLORS[TerminalColors.Color.WHITE];
        final float r = ((foreground >> 16) & 0xFF) / 255f;
        final float g = ((foreground >> 8) & 0xFF) / 255f;
        final float b = (foreground & 0xFF) / 255f;

        switch (terminal.cursorMode) {
            case TerminalColors.CursorMode.DEFAULT,
                    TerminalColors.CursorMode.BLINK_BLOCK,
                    TerminalColors.CursorMode.STEADY_BLOCK -> {
                buffer.addVertex(matrix, 0, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT, 0)
                        .setColor(r, g, b, 1);
                buffer.addVertex(matrix, Terminal.CHAR_WIDTH, 0, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 1);
            }
            case TerminalColors.CursorMode.BLINK_UNDERLINE,
                    TerminalColors.CursorMode.STEADY_UNDERLINE -> {
                buffer.addVertex(matrix, 0, 1, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, Terminal.CHAR_WIDTH, 1, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, Terminal.CHAR_WIDTH, 0, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 1);
            }
            case TerminalColors.CursorMode.BLINKING_BAR_LINE,
                    TerminalColors.CursorMode.STEADY_BAR_LINE -> {
                buffer.addVertex(matrix, 0, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, 1, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, 1, 0, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 1);
            }
        }

        MeshData rb = buffer.buildOrThrow();
        BufferUploader.drawWithShader(rb);

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        stack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }
}
