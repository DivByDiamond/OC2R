package li.cil.oc2.common.vm.terminal;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
import li.cil.oc2.common.vm.terminal.fonts.Glyph;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.concurrent.atomic.AtomicInteger;

@OnlyIn(Dist.CLIENT)
public class TerminalRenderer implements RendererModel, RendererView {
    private static final Logger RENDERER_LOGGER = LogManager.getLogger();

    public final Terminal terminal;
    public final VertexBuffer[] lines = new VertexBuffer[Terminal.HEIGHT];
    public final AtomicInteger dirty = new AtomicInteger(-1);

    public TerminalRenderer(final Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void render(final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock) {
        if (terminal.currentPrivateModeState.APPLICATION_SYNC) return;
        validateLineCache();
        renderBuffer(stack, projectionMatrix, renderingToBlock);

        boolean steady = switch (terminal.cursorMode) {
            case TerminalColors.CursorMode.STEADY_BLOCK, TerminalColors.CursorMode.STEADY_UNDERLINE, TerminalColors.CursorMode.STEADY_BAR_LINE -> true;
            default -> false;
        };

        if (steady || (System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500) {
            renderCursor(stack);
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
            if (vba[i] == vb) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void renderBuffer(final PoseStack stack, final Matrix4f projectionMatrix, boolean renderingToBlock) {
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

            final Matrix4f blockModelViewMatrix = RenderSystem.getModelViewMatrix();
            final Matrix4f blockProjectionMatrix = RenderSystem.getProjectionMatrix();

            for (final VertexBuffer line : lines) {
                if (line != null && !line.isInvalid()) {
                    try {
                        line.bind();
                        line.drawWithShader(blockModelViewMatrix, blockProjectionMatrix, shader);
                        VertexBuffer.unbind();
                    } catch (final Exception e) {
                        RENDERER_LOGGER.error("Failed to draw terminal line {}", findLineIndex(lines, line), e);
                    }
                }
            }

            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        } else {
            var modelViewMatrix = stack.last().pose();

            for (final VertexBuffer line : lines) {
                if (line != null && !line.isInvalid()) {
                    try {
                        line.bind();
                        line.drawWithShader(modelViewMatrix, projectionMatrix, shader);
                        VertexBuffer.unbind();
                    } catch (final Exception e) {
                        RENDERER_LOGGER.error("Failed to draw terminal line {}", findLineIndex(lines, line), e);
                    }
                }
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }

    @SuppressWarnings("resource")
    public void validateLineCache() {
        if (dirty.get() == 0) return;

        final int mask = dirty.getAndSet(0);
        for (int row = 0; row < lines.length; row++) {
            if ((mask & (1 << row)) == 0) continue;

            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            final Matrix4f matrix = new Matrix4f().translate(0, row * Terminal.CHAR_HEIGHT, 0);

            renderBackground(matrix, builder, row);
            renderForeground(matrix, builder, row);

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

    public void renderBackground(final Matrix4f matrix, final BufferBuilder buffer, final int row) {
        float backgroundStartX = -1;
        int backgroundColor = 0;
        float tx = 0f;
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        for (int col = 0, index = useAltBuffer ? row * Terminal.WIDTH : (row + (terminal.lastRowToDisplay - Terminal.HEIGHT)) * Terminal.WIDTH; col < Terminal.WIDTH; col++, index++) {
            final byte style = useAltBuffer ? terminal.altStyles[index] : terminal.styles[index];
            final boolean invertBackground = (style & Terminal.STYLE_INVERT_MASK) != 0;
            final ColorData color = !invertBackground ? useAltBuffer ? terminal.altColorsBackground[index] : terminal.colorsBackground[index] : useAltBuffer ? terminal.altColors[index] : terminal.colors[index];

            if ((style & Terminal.STYLE_HIDDEN_MASK) != 0) continue;

            final int[] palette = (style & Terminal.STYLE_DIM_MASK) != 0 ? TerminalColors.DIM_COLORS : TerminalColors.COLORS;
            int background = switch (color.Mode) {
                case SIXTEEN_COLOR -> palette[!invertBackground ? color.G : color.R];
                case TWO_FIFTY_SIX_COLOR -> TerminalColors.COLORS_256[!invertBackground ? color.G : color.R];
                case TRUE_COLOR -> color.ToInt();
                case SIXTEEN_COLOR_BRIGHT -> TerminalColors.BRIGHT_COLORS[!invertBackground ? color.G : color.R];
                case DEFAULT_BACKGROUND -> 0x000000;
            };

            final boolean hadBackground = backgroundStartX >= 0;
            final boolean hasBackground = background != 0x000000;
            if (!hadBackground && hasBackground) {
                backgroundStartX = tx;
                backgroundColor = background;
            } else if (hadBackground && (!hasBackground || backgroundColor != background)) {
                renderBackgroundRect(matrix, buffer, backgroundStartX, tx, backgroundColor);
                if (hasBackground) {
                    backgroundStartX = tx;
                    backgroundColor = background;
                } else {
                    backgroundStartX = -1;
                }
            }
            tx += Terminal.CHAR_WIDTH;
        }

        if (backgroundStartX >= 0) {
            renderBackgroundRect(matrix, buffer, backgroundStartX, tx, backgroundColor);
        }
    }

    public void renderBackgroundRect(final Matrix4f matrix, final BufferBuilder buffer, final float x0, final float x1, final int color) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        buffer.addVertex(matrix, x0, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x1, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x1, 0, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x0, 0, 0).setColor(r, g, b, 1).setUv(0, 0);
    }

    public void renderForeground(final Matrix4f matrix, final BufferBuilder buffer, final int row) {
        float tx = 0f;
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        for (int col = 0, index = useAltBuffer ? row * Terminal.WIDTH : (row + (terminal.lastRowToDisplay - Terminal.HEIGHT)) * Terminal.WIDTH; col < Terminal.WIDTH; col++, index++) {
            final byte style = useAltBuffer ? terminal.altStyles[index] : terminal.styles[index];
            final boolean invertBackground = (style & Terminal.STYLE_INVERT_MASK) != 0;
            final ColorData color = !invertBackground ? useAltBuffer ? terminal.altColors[index] : terminal.colors[index] : useAltBuffer ? terminal.altColorsBackground[index] : terminal.colorsBackground[index];

            if ((style & Terminal.STYLE_HIDDEN_MASK) != 0) continue;

            final int[] palette = (style & Terminal.STYLE_DIM_MASK) != 0 ? TerminalColors.DIM_COLORS : TerminalColors.COLORS;
            int foreground = switch (color.Mode) {
                case SIXTEEN_COLOR -> palette[!invertBackground ? color.R : color.G];
                case TWO_FIFTY_SIX_COLOR -> TerminalColors.COLORS_256[!invertBackground ? color.R : color.G];
                case TRUE_COLOR -> color.ToInt();
                case SIXTEEN_COLOR_BRIGHT -> TerminalColors.BRIGHT_COLORS[!invertBackground ? color.R : color.G];
                case DEFAULT_BACKGROUND -> 0x000000;
            };

            final int character = useAltBuffer ? terminal.altBuffer[index] : terminal.buffer[index];
            renderForegroundChar(matrix, buffer, tx, character, foreground, style);
            tx += Terminal.CHAR_WIDTH;
        }
    }

    public void renderForegroundChar(final Matrix4f matrix, final BufferBuilder buffer, final float offset, final int character, final int color, final byte style) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        if (isPrintableCharacter(character)) {
            FontHandling.FontStyle font = getFontStyle(style);
            Glyph glyph = FontHandling.getGlyph(character, font);

            if (font == FontHandling.FontStyle.ITALIC || font == FontHandling.FontStyle.BOLD_ITALIC) {
                buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(glyph.uStart, glyph.vEnd);
                buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH + 8, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(glyph.uEnd, glyph.vEnd);
                buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH + 8, 0, 0).setColor(r, g, b, 1).setUv(glyph.uEnd, glyph.vStart);
                buffer.addVertex(matrix, offset, 0, 0).setColor(r, g, b, 1).setUv(glyph.uStart, glyph.vStart);
            } else {
                buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(glyph.uStart, glyph.vEnd);
                buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(glyph.uEnd, glyph.vEnd);
                buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, 0, 0).setColor(r, g, b, 1).setUv(glyph.uEnd, glyph.vStart);
                buffer.addVertex(matrix, offset, 0, 0).setColor(r, g, b, 1).setUv(glyph.uStart, glyph.vStart);
            }
        }

        if ((style & Terminal.STYLE_UNDERLINE_MASK) != 0) {
            buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT - 3, 0).setColor(r, g, b, 1).setUv(0, 0);
            buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT - 3, 0).setColor(r, g, b, 1).setUv(0, 0);
            buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT - 2, 0).setColor(r, g, b, 1).setUv(0, 0);
            buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT - 2, 0).setColor(r, g, b, 1).setUv(0, 0);
        }
    }

    private FontHandling.FontStyle getFontStyle(byte style) {
        if ((style & Terminal.STYLE_BOLD_MASK) != 0 && (style & Terminal.STYLE_ITALIC_MASK) != 0)
            return FontHandling.FontStyle.BOLD_ITALIC;
        if ((style & Terminal.STYLE_BOLD_MASK) != 0)
            return FontHandling.FontStyle.BOLD;
        if ((style & Terminal.STYLE_ITALIC_MASK) != 0)
            return FontHandling.FontStyle.ITALIC;
        return FontHandling.FontStyle.REGULAR;
    }

    public void renderCursor(final PoseStack stack) {
        BufferUploader.reset();
        if (!terminal.currentPrivateModeState.DECTCEM) return;

        int globalY = terminal.lastRowToDisplayMax - (Terminal.HEIGHT - terminal.y);
        int localY = Terminal.HEIGHT + (globalY - terminal.lastRowToDisplay);
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        if (terminal.x < 0 || terminal.x >= Terminal.WIDTH || ((!useAltBuffer && localY < 0) || terminal.y < 0) || ((!useAltBuffer && localY >= Terminal.HEIGHT) || terminal.y >= Terminal.HEIGHT) || (!useAltBuffer && globalY > terminal.lastRowToDisplay)) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        stack.pushPose();
        stack.translate(terminal.x * Terminal.CHAR_WIDTH, (useAltBuffer ? terminal.y : localY) * Terminal.CHAR_HEIGHT, 0);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(stack.last().pose());
        RenderSystem.applyModelViewMatrix();

        final Matrix4f matrix = new Matrix4f();
        final BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        final int foreground = TerminalColors.COLORS[TerminalColors.Color.WHITE];
        final float r = ((foreground >> 16) & 0xFF) / 255f;
        final float g = ((foreground >> 8) & 0xFF) / 255f;
        final float b = (foreground & 0xFF) / 255f;

        switch (terminal.cursorMode) {
            case TerminalColors.CursorMode.DEFAULT, TerminalColors.CursorMode.BLINK_BLOCK, TerminalColors.CursorMode.STEADY_BLOCK -> {
                buffer.addVertex(matrix, 0, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, Terminal.CHAR_WIDTH, 0, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 1);
            }
            case TerminalColors.CursorMode.BLINK_UNDERLINE, TerminalColors.CursorMode.STEADY_UNDERLINE -> {
                buffer.addVertex(matrix, 0, 1, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, Terminal.CHAR_WIDTH, 1, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, Terminal.CHAR_WIDTH, 0, 0).setColor(r, g, b, 1);
                buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 1);
            }
            case TerminalColors.CursorMode.BLINKING_BAR_LINE, TerminalColors.CursorMode.STEADY_BAR_LINE -> {
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

    public static boolean isPrintableCharacter(final int ch) {
        return ch == 0 || (ch > ' ' && ch <= '~') || ch >= 177;
    }
}
