package li.cil.oc2.common.vm.terminal.render.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
//? if >=26.1 {
/*import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?} else {
import net.minecraft.client.renderer.GameRenderer;
//?}
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TerminalCursorRenderer {
    public static void renderCursor(final Terminal terminal, final PoseStack stack) {
        //? if <26.1 {
        BufferUploader.reset();
        //?}
        if (!terminal.currentPrivateModeState.DECTCEM) return;

        int globalY = terminal.lastRowToDisplayMax - (terminal.height - terminal.y);
        int localY = terminal.height + globalY - terminal.lastRowToDisplay;
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        if (!isCursorVisible(terminal, useAltBuffer, globalY, localY)) {
            return;
        }

        //? if <26.1 {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        //?}
        stack.pushPose();
        stack.translate(
                terminal.x * Terminal.CHAR_WIDTH,
                (useAltBuffer ? terminal.y : localY) * Terminal.CHAR_HEIGHT,
                0);

        //? if >=26.1 {
/*        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(stack.last().pose());
*///?} else {
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(stack.last().pose());
        RenderSystem.applyModelViewMatrix();
        //?}

        final Matrix4f matrix = new Matrix4f();
        final BufferBuilder buffer =
                Tesselator.getInstance()
                        .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Cursor color is fixed — it does not track OSC 4 (xterm reserves cursor color for
        // OSC 12): default foreground normally, default background (black) under DECSCNM.
        final int foreground = terminal.currentPrivateModeState.DECSCNM
                ? TerminalColors.defaultBackgroundRgb()
                : TerminalColors.defaultForegroundRgb(false);
        final float r = ((foreground >> 16) & 0xFF) / 255f;
        final float g = ((foreground >> 8) & 0xFF) / 255f;
        final float b = (foreground & 0xFF) / 255f;

        drawCursorShape(buffer, matrix, terminal.cursorMode, r, g, b);

        MeshData rb = buffer.buildOrThrow();
        //? if >=26.1 {
/*        // Blending, depth test without depth writes and no culling come with the debug quads
        // pipeline, which replaces the RenderSystem state calls of the 1.21 branch.
        RenderTypes.debugQuads().draw(rb);

        RenderSystem.getModelViewStack().popMatrix();
        stack.popPose();
*///?} else {
        BufferUploader.drawWithShader(rb);

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        stack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        //?}
    }

    private static boolean isCursorVisible(
            final Terminal terminal,
            final boolean useAltBuffer,
            final int globalY,
            final int localY) {
        return terminal.x >= 0
                && terminal.x < terminal.width
                && (useAltBuffer || localY >= 0)
                && terminal.y >= 0
                && (useAltBuffer || localY < terminal.height)
                && terminal.y < terminal.height
                && (useAltBuffer || globalY <= terminal.lastRowToDisplay);
    }

    private static void drawCursorShape(
            final BufferBuilder buffer,
            final Matrix4f matrix,
            final int cursorMode,
            final float r,
            final float g,
            final float b) {
        switch (cursorMode) {
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
            default -> {}
        }
    }
}