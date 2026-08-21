package li.cil.oc2.common.vm.terminal.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
import li.cil.oc2.common.vm.terminal.render.overlay.TerminalBackgroundRenderer;
import li.cil.oc2.common.vm.terminal.render.overlay.TerminalCursorRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TerminalRenderer implements RendererModel, RendererView {
    private static final Logger RENDERER_LOGGER = LogManager.getLogger();

    public final Terminal terminal;
    public final VertexBuffer[] lines = new VertexBuffer[Terminal.HEIGHT];
    public final AtomicInteger dirty = new AtomicInteger(-1);

    // Blink phase tracking: when the blink phase changes, lines containing blink-styled
    // chars must be rebuilt so the chars appear/disappear.
    private boolean lastBlinkPhase = false;

    public TerminalRenderer(final Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void render(
            final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock) {
        if (terminal.currentPrivateModeState.APPLICATION_SYNC) return;

        // Blink phase tracking: when the blink phase changes, mark lines containing
        // blink-styled chars dirty so they rebuild and the chars appear/disappear.
        final boolean blinkPhase = (System.currentTimeMillis() + terminal.hashCode()) % 1000 < 500;
        if (blinkPhase != lastBlinkPhase) {
            lastBlinkPhase = blinkPhase;
            final boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
            final int baseRow = useAltBuffer ? 0 : terminal.lastRowToDisplay - Terminal.HEIGHT;
            final byte[] styles = terminal.styles;
            final byte[] altStyles = terminal.altStyles;
            int mask = 0;
            for (int row = 0; row < Terminal.HEIGHT; row++) {
                final int rowBase = (baseRow + row) * Terminal.WIDTH;
                for (int col = 0; col < Terminal.WIDTH; col++) {
                    final int index = rowBase + col;
                    if ((useAltBuffer
                            ? (altStyles[index] & Terminal.STYLE_BLINK_MASK)
                            : (styles[index] & Terminal.STYLE_BLINK_MASK)) != 0) {
                        mask |= (1 << row);
                        break;
                    }
                }
            }
            if (mask != 0) {
                dirty.accumulateAndGet(mask, (a, b) -> a | b);
            }
        }

        validateLineCache();
        renderBuffer(stack, projectionMatrix, renderingToBlock);

        boolean steady = terminal.cursorMode == TerminalColors.CursorMode.STEADY_BLOCK
                || terminal.cursorMode == TerminalColors.CursorMode.STEADY_UNDERLINE
                || terminal.cursorMode == TerminalColors.CursorMode.STEADY_BAR_LINE;

        if (steady || (System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500) {
            TerminalCursorRenderer.renderCursor(terminal, stack);
        }
    }

    @Override
    public AtomicInteger getDirtyMask() {
        return dirty;
    }

    @Override
    public void close() {
        for (int i = 0; i < lines.length; i++) {
            final VertexBuffer line = lines[i];
            if (line != null) {
                line.close();
                lines[i] = null;
            }
        }
    }

    public int findLineIndex(VertexBuffer[] vba, VertexBuffer vb) {
        int i = 0;
        while (i < vba.length) {
            if (vba[i].equals(vb)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void renderBuffer(
            final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock) {
        final ShaderInstance shader = GameRenderer.getPositionTexColorShader();
        if (shader == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, FontHandling.getAtlas());

        if (renderingToBlock) {
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().mul(stack.last().pose());
            RenderSystem.applyModelViewMatrix();

            drawLines(shader, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        } else {
            drawLines(shader, stack.last().pose(), projectionMatrix);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }

    private void drawLines(
            final ShaderInstance shader,
            final Matrix4f modelViewMatrix,
            final Matrix4f projectionMatrix) {
        for (final VertexBuffer line : lines) {
            if (line != null && !line.isInvalid()) {
                try {
                    line.bind();
                    line.drawWithShader(modelViewMatrix, projectionMatrix, shader);
                    VertexBuffer.unbind();
                } catch (final Exception e) {
                    RENDERER_LOGGER.error(
                            "Failed to draw terminal line {}", findLineIndex(lines, line), e);
                }
            }
        }
    }

    public void validateLineCache() {
        if (dirty.get() == 0) return;

        final int mask = dirty.getAndSet(0);
        final Matrix4f matrix = new Matrix4f();
        for (int row = 0; row < lines.length; row++) {
            if ((mask & (1 << row)) == 0) continue;

            BufferBuilder builder =
                    Tesselator.getInstance()
                            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            matrix.identity().translate(0, row * Terminal.CHAR_HEIGHT, 0);

            TerminalBackgroundRenderer.renderBackground(terminal, matrix, builder, row);
            TerminalCharRenderer.renderForeground(terminal, matrix, builder, row);

            MeshData rb = builder.build();

            if (rb != null) {
                if (lines[row] == null) {
                    lines[row] = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                } else {
                    lines[row].close();
                    lines[row] = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                }

                if (!lines[row].isInvalid()) {
                    lines[row].bind();
                    lines[row].upload(rb);
                    VertexBuffer.unbind();
                }
            } else if (lines[row] != null) {
                lines[row].close();
                lines[row] = null;
            }
        }
    }
}