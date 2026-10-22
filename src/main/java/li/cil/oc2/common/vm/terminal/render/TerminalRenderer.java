package li.cil.oc2.common.vm.terminal.render;

//? if >=26.1 && <26.2 {
/*import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
*///?}
//? if <26.2 {
import com.mojang.blaze3d.systems.RenderSystem;
//?}
import com.mojang.blaze3d.vertex.*;
//? if >=26.1 {
/*import java.nio.ByteBuffer;
import java.util.Arrays;
*///?}
import java.util.concurrent.atomic.AtomicLong;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
//? if <26.2 {
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
//?}
import li.cil.oc2.common.vm.terminal.render.overlay.TerminalBackgroundRenderer;
import li.cil.oc2.common.vm.terminal.render.overlay.TerminalCursorRenderer;
//? if >=26.1 && <26.2 {
/*import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
*///?}
//? if <26.1 {
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
//?}
//? if <26.2 {
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//?}
import org.joml.Matrix4f;
//? if >=26.1 && <26.2 {
/*import org.lwjgl.system.MemoryUtil;
*///?}

@OnlyIn(Dist.CLIENT)
public class TerminalRenderer implements RendererModel, RendererView {
    //? if <26.2 {
    private static final Logger RENDERER_LOGGER = LogManager.getLogger();

    //?}
    public final Terminal terminal;

    //? if >=26.1 {
    /*// 26.x has no retained VertexBuffer. Each dirty row's vertices are kept as raw bytes and the
    // rows are replayed into one mesh drawn through RenderType each frame.
    private record RowMesh(byte[] data, int vertexCount, int backgroundVertexCount) {}

    //? if <26.2 {
    private static final RenderPipeline PIPELINE =
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/oc2r_terminal")
                    .withCull(false)
                    .withDepthStencilState(
                            new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build();

    private static RenderType renderType;

    //?}
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

//? if >=26.1 {
    /*// GUIs on 26.x only extract render state, so the terminal cannot be drawn immediately: the
    // screen takes this snapshot while extracting and replays it when the GUI is rendered.
    public record GuiSnapshot(
            byte[] data, int vertexCount, int backgroundVertexCount, byte[] cursorData) {
        public boolean cursorVisible() {
            return cursorData.length > 0;
        }

        public int textVertexCount() {
            return vertexCount - backgroundVertexCount;
        }

        public void replayBackground(final VertexConsumer consumer, final org.joml.Matrix3x2fc pose) {
            replayGui(consumer, pose, 0, backgroundVertexCount, false);
        }

        private static final int VERTEX_SIZE = 24; // POSITION_TEX_COLOR: 3 + 2 floats, 1 int

        public void replayText(final VertexConsumer consumer, final org.joml.Matrix3x2fc pose) {
            replayGui(consumer, pose, backgroundVertexCount, vertexCount, true);
        }

        private void replayGui(
                final VertexConsumer consumer, final org.joml.Matrix3x2fc pose,
                final int start, final int end, final boolean textured) {
            final java.nio.ByteBuffer buffer =
                    java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.nativeOrder());
            final org.joml.Vector2f position = new org.joml.Vector2f();
            for (int i = start; i < end; i++) {
                final int base = i * VERTEX_SIZE;
                pose.transformPosition(buffer.getFloat(base), buffer.getFloat(base + 4), position);
                final int abgr = buffer.getInt(base + 20);
                consumer.addVertex(position.x, position.y, 0f)
                        .setColor(abgr & 0xFF, (abgr >> 8) & 0xFF, (abgr >> 16) & 0xFF, abgr >>> 24);
                if (textured) {
                    consumer.setUv(buffer.getFloat(base + 12), buffer.getFloat(base + 16));
                }
            }
        }

        // World replay: a 26.x block entity renderer only collects geometry, so the rows are
        // handed to a SubmitNodeCollector instead of a GUI element. Same bytes, but transformed
        // by the pose captured when the node was submitted (3D, includes the block face pose).
        public void replayText(final PoseStack.Pose pose, final VertexConsumer consumer) {
            replayWorld(pose, consumer, backgroundVertexCount, vertexCount, true);
        }

        public void replayBackground(final PoseStack.Pose pose, final VertexConsumer consumer) {
            replayWorld(pose, consumer, 0, backgroundVertexCount, false);
        }

        private void replayWorld(final PoseStack.Pose pose, final VertexConsumer consumer,
                final int start, final int end, final boolean textured) {
            final java.nio.ByteBuffer buffer =
                    java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.nativeOrder());
            for (int i = start; i < end; i++) {
                final int base = i * VERTEX_SIZE;
                final int abgr = buffer.getInt(base + 20);
                consumer.addVertex(
                                pose,
                                buffer.getFloat(base),
                                buffer.getFloat(base + 4),
                                buffer.getFloat(base + 8))
                        .setColor(abgr & 0xFF, (abgr >> 8) & 0xFF, (abgr >> 16) & 0xFF, abgr >>> 24);
                if (textured) {
                    consumer.setUv(buffer.getFloat(base + 12), buffer.getFloat(base + 16));
                }
            }
        }

        public void replayCursor(
                final VertexConsumer consumer,
                final org.joml.Matrix3x2fc pose) {
            replayCursor(new Matrix4f()
                    .m00(pose.m00()).m01(pose.m01())
                    .m10(pose.m10()).m11(pose.m11())
                    .m30(pose.m20()).m31(pose.m21()), consumer);
        }

        public void replayCursor(final PoseStack.Pose pose, final VertexConsumer consumer) {
            replayCursor(pose.pose(), consumer);
        }

        private void replayCursor(final Matrix4f pose, final VertexConsumer consumer) {
            final ByteBuffer buffer = java.nio.ByteBuffer.wrap(cursorData)
                    .order(java.nio.ByteOrder.nativeOrder());
            for (int base = 0; base < cursorData.length; base += 16) {
                final int abgr = buffer.getInt(base + 12);
                consumer.addVertex(pose, buffer.getFloat(base), buffer.getFloat(base + 4), buffer.getFloat(base + 8))
                        .setColor(abgr & 0xFF, (abgr >> 8) & 0xFF, (abgr >> 16) & 0xFF, abgr >>> 24);
            }
        }
    }

    // Null if nothing is to be drawn this frame (frame dropped, application sync or empty).
    @javax.annotation.Nullable
    public GuiSnapshot captureForGui() {
        if (!prepareFrame()) {
            return null;
        }

        int vertexCount = 0;
        int byteCount = 0;
        for (final RowMesh line : lines) {
            if (line != null) {
                vertexCount += line.vertexCount();
                byteCount += line.data().length;
            }
        }

        final byte[] data = new byte[byteCount];
        int backgroundVertexCount = 0;
        int offset = 0;
        // Backgrounds use a solid-color GUI pipeline; keep them before all glyphs so the GUI
        // renderer can batch each pass without sampling the font atlas for background colors.
        for (final RowMesh line : lines) {
            if (line != null) {
                final int size = line.backgroundVertexCount() * GuiSnapshot.VERTEX_SIZE;
                System.arraycopy(line.data(), 0, data, offset, size);
                offset += size;
                backgroundVertexCount += line.backgroundVertexCount();
            }
        }
        for (final RowMesh line : lines) {
            if (line != null) {
                final int start = line.backgroundVertexCount() * GuiSnapshot.VERTEX_SIZE;
                final int size = line.data().length - start;
                System.arraycopy(line.data(), start, data, offset, size);
                offset += size;
            }
        }

        byte[] cursorData = new byte[0];
        if (isCursorPhaseVisible()) {
            // Each iteration owns distinct geometry, coordinates or serialized state.
            try (ByteBufferBuilder bytes = new ByteBufferBuilder(256)) { // NOPMD
                //? if >=26.2 {
                final BufferBuilder builder = new BufferBuilder(bytes, com.mojang.blaze3d.PrimitiveTopology.QUADS,
                        DefaultVertexFormat.POSITION_COLOR);
                //?} else {
                final BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS,
                        DefaultVertexFormat.POSITION_COLOR);
                //?}
                TerminalCursorRenderer.buildCursor(terminal, builder, new Matrix4f());
                try (MeshData mesh = builder.build()) {
                    if (mesh != null) {
                        final ByteBuffer vertices = mesh.vertexBuffer().duplicate();
                        cursorData = new byte[vertices.remaining()];
                        vertices.get(cursorData);
                    }
                }
            }
        }
        return new GuiSnapshot(data, vertexCount, backgroundVertexCount, cursorData);
    }
*///?}

    @Override
    public void render(final PoseStack stack,
            final Matrix4f projectionMatrix, boolean renderingToBlock) {
        if (!prepareFrame()) {
            return;
        }

        renderBuffer(stack, projectionMatrix, renderingToBlock);

        if (isCursorPhaseVisible()) {
            TerminalCursorRenderer.renderCursor(terminal, stack);
        }
    }

    private boolean isCursorPhaseVisible() {
        final boolean steady = terminal.cursorMode == TerminalColors.CursorMode.STEADY_BLOCK
                || terminal.cursorMode == TerminalColors.CursorMode.STEADY_UNDERLINE
                || terminal.cursorMode == TerminalColors.CursorMode.STEADY_BAR_LINE;
        return steady || Math.floorMod(System.currentTimeMillis() + terminal.hashCode(), 1000) > 500;
    }

    // Captures the frame and rebuilds the dirty rows; false if the frame was dropped.
    private boolean prepareFrame() { // NOPMD: blink phase tracking + frame capture
        if (terminal.currentPrivateModeState.APPLICATION_SYNC) return false;

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
            return false; // resize storm: skip this frame entirely; nothing has been mutated yet
        }

        // Dynamic height: reallocate the lines array if the terminal's height changed
        // (e.g. via TerminalDiff.apply calling resizeHeight). Close old buffers first.
        if (lines.length != frame.height()) {
            //? if >=26.1 {
            /*lines = new RowMesh[frame.height()];
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
        return true;
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
        /*Arrays.fill(lines, null);
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
//? if >=26.2 {
    /*// 26.2 removed RenderType.draw(MeshData) and the tessellator; the terminal text is not drawn
    // there yet, so the render entry point is a no-op.
    public void renderBuffer(
            final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock) {}

*///?} else if >=26.1 {
    /*// The projection matrix argument has no counterpart on 26.x: RenderType draws with the
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
//? if >=26.2 {
    /*// 26.2 removed Tesselator, but BufferBuilder can still produce CPU-side mesh data. Cache each
    // dirty row as bytes; the block renderer replays those bytes through its submit pipeline.
    public void validateLineCache(final FrameState frame) {
        if (dirty.get() == 0) return;

        final long mask = dirty.getAndSet(0L);
        final Matrix4f matrix = new Matrix4f();
        final int blinkSeed = terminal.hashCode();
        for (int row = 0; row < lines.length; row++) {
            if ((mask & (1L << row)) == 0) continue;

            matrix.identity().translate(0, row * Terminal.CHAR_HEIGHT, 0);
            final byte[] background;
            final int backgroundVertices;
            // Each iteration owns distinct geometry, coordinates or serialized state.
            try (ByteBufferBuilder bytes = new ByteBufferBuilder(256)) { // NOPMD
                // Each iteration owns distinct geometry, coordinates or serialized state.
                final BufferBuilder builder = new BufferBuilder(// NOPMD
                        bytes,
                        com.mojang.blaze3d.PrimitiveTopology.QUADS,
                        DefaultVertexFormat.POSITION_TEX_COLOR);
                TerminalBackgroundRenderer.renderBackground(frame, matrix, builder, row, blinkSeed);
                try (MeshData mesh = builder.build()) {
                    if (mesh != null) {
                        final ByteBuffer vertices = mesh.vertexBuffer().duplicate();
                        // Each iteration owns distinct geometry, coordinates or serialized state.
                        background = new byte[vertices.remaining()]; // NOPMD
                        vertices.get(background);
                        backgroundVertices = mesh.drawState().vertexCount();
                    } else {
                        // Each iteration owns distinct geometry, coordinates or serialized state.
                        background = new byte[0]; // NOPMD
                        backgroundVertices = 0;
                    }
                }
            }

            final byte[] text;
            final int textVertices;
            // Each iteration owns distinct geometry, coordinates or serialized state.
            try (ByteBufferBuilder bytes = new ByteBufferBuilder(256)) { // NOPMD
                // Each iteration owns distinct geometry, coordinates or serialized state.
                final BufferBuilder builder = new BufferBuilder(// NOPMD
                        bytes,
                        com.mojang.blaze3d.PrimitiveTopology.QUADS,
                        DefaultVertexFormat.POSITION_TEX_COLOR);
                TerminalCharRenderer.renderForeground(frame, matrix, builder, row, blinkSeed);
                try (MeshData mesh = builder.build()) {
                    if (mesh != null) {
                        final ByteBuffer vertices = mesh.vertexBuffer().duplicate();
                        // Each iteration owns distinct geometry, coordinates or serialized state.
                        text = new byte[vertices.remaining()]; // NOPMD
                        vertices.get(text);
                        textVertices = mesh.drawState().vertexCount();
                    } else {
                        // Each iteration owns distinct geometry, coordinates or serialized state.
                        text = new byte[0]; // NOPMD
                        textVertices = 0;
                    }
                }
            }

            if (backgroundVertices + textVertices == 0) {
                lines[row] = null;
            } else {
                final byte[] data = Arrays.copyOf(background, background.length + text.length);
                System.arraycopy(text, 0, data, background.length, text.length);
                lines[row] = new RowMesh(
                        data, backgroundVertices + textVertices, backgroundVertices);
            }
        }
    }
*///?} else if >=26.1 {
    /*public void validateLineCache(final FrameState frame) {
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
            // Each iteration owns distinct geometry, coordinates or serialized state.
            byte[] background = new byte[0]; // NOPMD
            int backgroundVertices = 0;
            try (MeshData mesh = builder.build()) {
                if (mesh != null) {
                    final ByteBuffer vertices = mesh.vertexBuffer().duplicate();
                    // Each iteration owns distinct geometry, coordinates or serialized state.
                    background = new byte[vertices.remaining()]; // NOPMD
                    vertices.get(background);
                    backgroundVertices = mesh.drawState().vertexCount();
                }
            }
            builder = Tesselator.getInstance()
                    .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            TerminalCharRenderer.renderForeground(frame, matrix, builder, row, blinkSeed);

            try (MeshData rb = builder.build()) {
                if (rb != null) {
                    final ByteBuffer vertices = rb.vertexBuffer().duplicate();
                    final byte[] data = Arrays.copyOf(background, background.length + vertices.remaining());
                    vertices.get(data, background.length, vertices.remaining());
                    lines[row] = new RowMesh(data,
                            backgroundVertices + rb.drawState().vertexCount(), backgroundVertices);
                } else {
                    lines[row] = backgroundVertices == 0
                            ? null : new RowMesh(background, backgroundVertices, backgroundVertices);
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
