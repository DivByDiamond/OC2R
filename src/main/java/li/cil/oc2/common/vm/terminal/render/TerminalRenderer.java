package li.cil.oc2.common.vm.terminal.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.concurrent.atomic.AtomicLong;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
import li.cil.oc2.common.vm.terminal.render.overlay.TerminalBackgroundRenderer;
import li.cil.oc2.common.vm.terminal.render.overlay.TerminalCursorRenderer;
//? if >=26.1 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.lwjgl.system.MemoryUtil;
*///?} else {
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
//?}
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TerminalRenderer implements RendererModel, RendererView {
    private static final Logger RENDERER_LOGGER = LogManager.getLogger();

    public final Terminal terminal;
    //? if >=26.1 {
/*    // 26.x has no retained VertexBuffer. Each dirty row's vertices are kept as raw bytes and the
    // rows are replayed into one mesh drawn through RenderType each frame.
    private record RowMesh(byte[] data, int vertexCount) {}

    private static final RenderPipeline PIPELINE =
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/oc2r_terminal")
                    .withCull(false)
                    .withDepthStencilState(
                            new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build();

    private static RenderType renderType;

    private RowMesh[] lines = new RowMesh[Terminal.HEIGHT];
*///?} else {
    private VertexBuffer[] lines = new VertexBuffer[Terminal.HEIGHT];
    //?}
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
        // capture guarantees the fields belong to one committed geometry. captureRetrying
        // returns null if every attempt landed inside a commit stretch — drop the frame and
        // let the next one render the committed state. There is deliberately NO torn-frame
        // fallback: capture never returns mixed data, and rendering a mixed capture would
        // re-open the structural tear class the seqlock closed (an earlier comment promised
        // exactly that fallback — it never existed, and the two-probe code could NPE here).
        final FrameState frame = captureFrame();
        if (frame == null) {
            return; // resize storm: skip this frame entirely; nothing has been mutated yet
        }

        // Dynamic height: reallocate the lines array if the terminal's height changed
        // (e.g. via TerminalDiff.apply calling resizeHeight). Close old buffers first.
        if (lines.length != frame.height()) {
            //? if >=26.1 {
/*            lines = new RowMesh[frame.height()];
*///?} else {
            for (final VertexBuffer line : lines) {
                if (line != null) line.close();
            }
            lines = new VertexBuffer[frame.height()];
            //?}
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
     * Capture one frame's terminal state via the seqlock with a bounded retry; null means the
     * geometry was committing across every attempt (the caller drops the frame).
     */
    private FrameState captureFrame() {
        return FrameState.captureRetrying(terminal, 2);
    }

    @Override
    public AtomicLong getDirtyMask() {
        return dirty;
    }

    @Override
    public void close() {
        //? if >=26.1 {
/*        Arrays.fill(lines, null);
*///?} else {
        for (int i = 0; i < lines.length; i++) {
            final VertexBuffer line = lines[i];
            if (line != null) {
                line.close();
                lines[i] = null;
            }
        }
        //?}
    }

//? if <26.1 {
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

//?}
//? if >=26.1 {
/*    // The projection matrix argument has no counterpart on 26.x: RenderType draws with the
    // projection the caller has bound. The pose is applied through the model-view stack.
    public void renderBuffer(
            final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock) {
        int vertexCount = 0;
        int byteCount = 0;
        for (final RowMesh line : lines) {
            if (line != null) {
                vertexCount += line.vertexCount();
                byteCount += line.data().length;
            }
        }
        if (vertexCount == 0) {
            return;
        }

        if (renderType == null) {
            renderType =
                    RenderType.create(
                            "oc2r_terminal",
                            RenderSetup.builder(PIPELINE)
                                    .withTexture("Sampler0", FontHandling.getAtlas())
                                    .createRenderSetup());
        }

        final var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(stack.last().pose());
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(byteCount)) {
            final ByteBuffer target = MemoryUtil.memByteBuffer(bytes.reserve(byteCount), byteCount);
            for (final RowMesh line : lines) {
                if (line != null) {
                    target.put(line.data());
                }
            }
            final MeshData mesh =
                    new MeshData(
                            bytes.build(),
                            new MeshData.DrawState(
                                    DefaultVertexFormat.POSITION_TEX_COLOR,
                                    vertexCount,
                                    VertexFormat.Mode.QUADS.indexCount(vertexCount),
                                    VertexFormat.Mode.QUADS,
                                    VertexFormat.IndexType.least(vertexCount)));
            renderType.draw(mesh);
        } catch (final Exception e) {
            RENDERER_LOGGER.error("Failed to draw terminal", e);
        } finally {
            modelViewStack.popMatrix();
        }
    }

*///?} else {
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

//?}
//? if >=26.1 {
/*    public void validateLineCache(final FrameState frame) {
        if (dirty.get() == 0) return;

        final long mask = dirty.getAndSet(0L);
        final Matrix4f matrix = new Matrix4f();
        // Blink phase seed: the terminal's identity, see the 1.21 branch for the reasoning.
        final int blinkSeed = terminal.hashCode();
        for (int row = 0; row < lines.length; row++) {
            if ((mask & (1L << row)) == 0) continue;

            BufferBuilder builder =
                    Tesselator.getInstance()
                            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            matrix.identity().translate(0, row * Terminal.CHAR_HEIGHT, 0);

            TerminalBackgroundRenderer.renderBackground(frame, matrix, builder, row, blinkSeed);
            TerminalCharRenderer.renderForeground(frame, matrix, builder, row, blinkSeed);

            try (MeshData rb = builder.build()) {
                if (rb != null) {
                    final ByteBuffer vertices = rb.vertexBuffer().duplicate();
                    final byte[] data = new byte[vertices.remaining()];
                    vertices.get(data);
                    lines[row] = new RowMesh(data, rb.drawState().vertexCount());
                } else {
                    lines[row] = null;
                }
            }
        }
    }
*///?} else {
    public void validateLineCache(final FrameState frame) {
        if (dirty.get() == 0) return;

        final long mask = dirty.getAndSet(0L);
        final Matrix4f matrix = new Matrix4f();
        // Blink phase seed: the terminal's identity — stable across buffer reallocs, and the
        // SAME seed the blink-dirty loop and cursor gate use, so row rebuilds and the visible
        // phase stay in step. (The frame's buffer identity would re-seed every resize.)
        final int blinkSeed = terminal.hashCode();
        for (int row = 0; row < lines.length; row++) {
            if ((mask & (1L << row)) == 0) continue;

            BufferBuilder builder =
                    Tesselator.getInstance()
                            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            matrix.identity().translate(0, row * Terminal.CHAR_HEIGHT, 0);

            TerminalBackgroundRenderer.renderBackground(frame, matrix, builder, row, blinkSeed);
            TerminalCharRenderer.renderForeground(frame, matrix, builder, row, blinkSeed);

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
//?}
}