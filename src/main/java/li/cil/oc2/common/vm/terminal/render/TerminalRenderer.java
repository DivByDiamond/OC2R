package li.cil.oc2.common.vm.terminal.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.concurrent.atomic.AtomicLong;
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
    private VertexBuffer[] lines = new VertexBuffer[Terminal.HEIGHT];
    public final AtomicLong dirty = new AtomicLong(-1L);

    // Blink phase tracking: when the blink phase changes, lines containing blink-styled
    // chars must be rebuilt so the chars appear/disappear.
    private boolean lastBlinkPhase = false;

    public TerminalRenderer(final Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void render(final PoseStack stack, // NOPMD: blink phase tracking + render dispatch
            final Matrix4f projectionMatrix, boolean renderingToBlock) {
        if (terminal.currentPrivateModeState.APPLICATION_SYNC) return;

        // One consistent frame capture under the geometry seqlock (§36 M4): the resize paths
        // bump the version around each commit stretch, so an even version before AND after the
        // capture guarantees the fields belong to one committed geometry. Two attempts; if the
        // terminal is being resized continuously we render the (possibly mixed) second capture
        // rather than dropping the frame — the tear is one frame, next frame repaints.
        FrameState frame = captureFrame();
        if (frame == null) {
            frame = captureFrame();
        }

        // Dynamic height: reallocate the lines array if the terminal's height changed
        // (e.g. via TerminalDiff.apply calling resizeHeight). Close old buffers first.
        if (lines.length != frame.height()) {
            for (final VertexBuffer line : lines) {
                if (line != null) line.close();
            }
            lines = new VertexBuffer[frame.height()];
            dirty.set(-1L);
        }

        // Blink phase tracking: when the blink phase changes, mark lines containing
        // blink-styled chars dirty so they rebuild and the chars appear/disappear.
        final boolean blinkPhase = Math.floorMod(System.currentTimeMillis() + terminal.hashCode(), 1000) < 500;
        if (blinkPhase != lastBlinkPhase) {
            lastBlinkPhase = blinkPhase;
            final int baseRow = frame.useAltBuffer() ? 0 : frame.lastRowToDisplay() - frame.height();
            final byte[] activeStyles = frame.useAltBuffer() ? frame.altStyles() : frame.styles();
            long mask = 0;
            for (int row = 0; row < frame.height(); row++) {
                final int rowBase = (baseRow + row) * frame.width();
                // Torn mid-resize read (§36 M4): skip rows that don't fit the captured
                // geometry instead of indexing out of bounds. Next frame repaints.
                if (rowBase < 0 || rowBase + frame.width() > activeStyles.length) {
                    continue;
                }
                for (int col = 0; col < frame.width(); col++) {
                    if ((activeStyles[rowBase + col] & Terminal.STYLE_BLINK_MASK) != 0) {
                        mask |= (1L << row);
                        break;
                    }
                }
            }
            if (mask != 0) {
                dirty.accumulateAndGet(mask, (a, b) -> a | b);
            }
        }

        validateLineCache(frame);
        renderBuffer(stack, projectionMatrix, renderingToBlock);

        boolean steady = terminal.cursorMode == TerminalColors.CursorMode.STEADY_BLOCK
                || terminal.cursorMode == TerminalColors.CursorMode.STEADY_UNDERLINE
                || terminal.cursorMode == TerminalColors.CursorMode.STEADY_BAR_LINE;

        if (steady || Math.floorMod(System.currentTimeMillis() + terminal.hashCode(), 1000) > 500) {
            TerminalCursorRenderer.renderCursor(terminal, stack);
        }
    }

    /**
     * Capture one frame's terminal state via the seqlock; null means the geometry moved
     * mid-capture (the caller retries once, then renders anyway).
     */
    private FrameState captureFrame() {
        return FrameState.capture(terminal);
    }

    @Override
    public AtomicLong getDirtyMask() {
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

    private int findLineIndex(VertexBuffer[] vba, VertexBuffer vb) {
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

    public void validateLineCache(final FrameState frame) {
        if (dirty.get() == 0) return;

        final long mask = dirty.getAndSet(0L);
        final Matrix4f matrix = new Matrix4f();
        for (int row = 0; row < lines.length; row++) {
            if ((mask & (1L << row)) == 0) continue;

            BufferBuilder builder =
                    Tesselator.getInstance()
                            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            matrix.identity().translate(0, row * Terminal.CHAR_HEIGHT, 0);

            TerminalBackgroundRenderer.renderBackground(frame, matrix, builder, row);
            TerminalCharRenderer.renderForeground(frame, matrix, builder, row);

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