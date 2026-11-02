package li.cil.oc2.common.vm.terminal.render.overlay;

//? if <26.2 {
import com.mojang.blaze3d.systems.RenderSystem;
//?}
import com.mojang.blaze3d.vertex.*;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.render.LineScale;
//? if >=26.1 && <26.2 {
/*import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?}
//? if <26.1 {
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TerminalCursorRenderer {
//? if >=26.2 {
/*    // 26.2 removed RenderType.draw(MeshData) and the tessellator; the cursor is not drawn there
    // yet, so the render entry point is a no-op.
    public static void renderCursor(final Terminal terminal, final PoseStack stack) {}

*///?} else {
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
        final LineScale scale = cursorScale(terminal, useAltBuffer, localY);
        stack.pushPose();
        stack.translate(
                terminal.x * Terminal.CHAR_WIDTH * scale.xScale(),
                (useAltBuffer ? terminal.y : localY) * Terminal.CHAR_HEIGHT,
                0);

        //? if >=26.1 {
        /*RenderSystem.getModelViewStack().pushMatrix();
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

        drawCursorShape(buffer, matrix, scale, terminal.cursorMode, r, g, b);

        MeshData rb = buffer.buildOrThrow();
        //? if >=26.1 {
        /*// Blending, depth test without depth writes and no culling come with the debug quads
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
//?}

//? if >=26.1 {
    /*// Cursor quads for a GUI element (POSITION_COLOR); `base` maps terminal pixels to the GUI.
    public static void buildCursor(
            final Terminal terminal, final VertexConsumer consumer, final Matrix4f base) {
        if (!terminal.currentPrivateModeState.DECTCEM) return;

        final int globalY = terminal.lastRowToDisplayMax - (terminal.height - terminal.y);
        final int localY = terminal.height + globalY - terminal.lastRowToDisplay;
        final boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        if (!isCursorVisible(terminal, useAltBuffer, globalY, localY)) {
            return;
        }

        final int foreground = terminal.currentPrivateModeState.DECSCNM
                ? TerminalColors.defaultBackgroundRgb()
                : TerminalColors.defaultForegroundRgb(false);
        final LineScale scale = cursorScale(terminal, useAltBuffer, localY);
        final Matrix4f matrix = new Matrix4f(base).translate(
                terminal.x * Terminal.CHAR_WIDTH * scale.xScale(),
                (useAltBuffer ? terminal.y : localY) * Terminal.CHAR_HEIGHT,
                0);
        drawCursorShape(
                consumer,
                matrix,
                scale,
                terminal.cursorMode,
                ((foreground >> 16) & 0xFF) / 255f,
                ((foreground >> 8) & 0xFF) / 255f,
                (foreground & 0xFF) / 255f);
    }

*///?}
    private static boolean isCursorVisible(
            final Terminal terminal,
            final boolean useAltBuffer,
            final int globalY,
            final int localY) {
        return terminal.x >= 0
                && terminal.x < cursorScale(terminal, useAltBuffer, localY).visibleColumns(terminal.width)
                && (useAltBuffer || localY >= 0)
                && terminal.y >= 0
                && (useAltBuffer || localY < terminal.height)
                && terminal.y < terminal.height
                && (useAltBuffer || globalY <= terminal.lastRowToDisplay);
    }

    /** Line scale of the row holding the cursor; {@code localY} is its row in the visible window. */
    private static LineScale cursorScale(
            final Terminal terminal, final boolean useAltBuffer, final int localY) {
        return useAltBuffer
                ? LineScale.of(terminal.altLineAttrs, terminal.y)
                : LineScale.of(terminal.lineAttrs, terminal.lastRowToDisplay - terminal.height + localY);
    }

    // Quad in cell space; the line scale widens it and, on double-height rows, doubles and crops it
    // to the half owned by the cursor's row.
    private static void cursorQuad(
            final VertexConsumer buffer,
            final Matrix4f matrix,
            final LineScale scale,
            final float x0,
            final float y0,
            final float x1,
            final float y1,
            final float r,
            final float g,
            final float b) {
        final float sy0 = Math.max(0f, scale.mapY(y0, Terminal.CHAR_HEIGHT));
        final float sy1 = Math.min(Terminal.CHAR_HEIGHT, scale.mapY(y1, Terminal.CHAR_HEIGHT));
        if (sy0 >= sy1) return;
        final float sx0 = x0 * scale.xScale();
        final float sx1 = x1 * scale.xScale();
        buffer.addVertex(matrix, sx0, sy1, 0).setColor(r, g, b, 1);
        buffer.addVertex(matrix, sx1, sy1, 0).setColor(r, g, b, 1);
        buffer.addVertex(matrix, sx1, sy0, 0).setColor(r, g, b, 1);
        buffer.addVertex(matrix, sx0, sy0, 0).setColor(r, g, b, 1);
    }

    private static void drawCursorShape(
            final VertexConsumer buffer,
            final Matrix4f matrix,
            final LineScale scale,
            final int cursorMode,
            final float r,
            final float g,
            final float b) {
        switch (cursorMode) {
            case TerminalColors.CursorMode.DEFAULT,
                    TerminalColors.CursorMode.BLINK_BLOCK,
                    TerminalColors.CursorMode.STEADY_BLOCK ->
                    cursorQuad(buffer, matrix, scale, 0, 0, Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT, r, g, b);
            case TerminalColors.CursorMode.BLINK_UNDERLINE,
                    TerminalColors.CursorMode.STEADY_UNDERLINE ->
                    cursorQuad(buffer, matrix, scale, 0, 0, Terminal.CHAR_WIDTH, 1, r, g, b);
            case TerminalColors.CursorMode.BLINKING_BAR_LINE,
                    TerminalColors.CursorMode.STEADY_BAR_LINE ->
                    cursorQuad(buffer, matrix, scale, 0, 0, 1, Terminal.CHAR_HEIGHT, r, g, b);
            default -> {}
        }
    }
}
