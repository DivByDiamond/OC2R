package li.cil.oc2.common.vm.terminal.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
import li.cil.oc2.common.vm.terminal.fonts.Glyph;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TerminalCharRenderer {
    static void renderForeground(
            final Terminal terminal,
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final int row) {
        float tx = 0f;
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        int index =
                useAltBuffer
                        ? row * Terminal.WIDTH
                        : (row + (terminal.lastRowToDisplay - Terminal.HEIGHT)) * Terminal.WIDTH;
        for (int col = 0; col < Terminal.WIDTH; col++, index++) {
            final byte style = useAltBuffer ? terminal.altStyles[index] : terminal.styles[index];
            final boolean invertBackground = (style & Terminal.STYLE_INVERT_MASK) != 0;
            final ColorData color =
                    !invertBackground
                            ? useAltBuffer ? terminal.altColors[index] : terminal.colors[index]
                            : useAltBuffer
                                    ? terminal.altColorsBackground[index]
                                    : terminal.colorsBackground[index];

            if ((style & Terminal.STYLE_HIDDEN_MASK) != 0) continue;

            final int[] palette =
                    (style & Terminal.STYLE_DIM_MASK) != 0
                            ? TerminalColors.DIM_COLORS
                            : TerminalColors.COLORS;
            int foreground =
                    switch (color.Mode) {
                        case SIXTEEN_COLOR -> palette[!invertBackground ? color.R : color.G];
                        case TWO_FIFTY_SIX_COLOR ->
                                TerminalColors.COLORS_256[!invertBackground ? color.R : color.G];
                        case TRUE_COLOR -> color.ToInt();
                        case SIXTEEN_COLOR_BRIGHT ->
                                TerminalColors.BRIGHT_COLORS[!invertBackground ? color.R : color.G];
                        case DEFAULT_BACKGROUND -> 0x000000;
                        default -> throw new AssertionError(color.Mode);
                    };

            final int character = useAltBuffer ? terminal.altBuffer[index] : terminal.buffer[index];
            renderForegroundChar(matrix, buffer, tx, character, foreground, style);
            tx += Terminal.CHAR_WIDTH;
        }
    }

    static void renderForegroundChar(
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final float offset,
            final int character,
            final int color,
            final byte style) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        if (isPrintableCharacter(character)) {
            FontHandling.FontStyle font = getFontStyle(style);
            Glyph glyph = FontHandling.getGlyph(character, font);

            if (font == FontHandling.FontStyle.ITALIC
                    || font == FontHandling.FontStyle.BOLD_ITALIC) {
                buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT, 0)
                        .setColor(r, g, b, 1)
                        .setUv(glyph.uStart, glyph.vEnd);
                buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH + 8, Terminal.CHAR_HEIGHT, 0)
                        .setColor(r, g, b, 1)
                        .setUv(glyph.uEnd, glyph.vEnd);
                buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH + 8, 0, 0)
                        .setColor(r, g, b, 1)
                        .setUv(glyph.uEnd, glyph.vStart);
                buffer.addVertex(matrix, offset, 0, 0)
                        .setColor(r, g, b, 1)
                        .setUv(glyph.uStart, glyph.vStart);
            } else {
                buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT, 0)
                        .setColor(r, g, b, 1)
                        .setUv(glyph.uStart, glyph.vEnd);
                buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT, 0)
                        .setColor(r, g, b, 1)
                        .setUv(glyph.uEnd, glyph.vEnd);
                buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, 0, 0)
                        .setColor(r, g, b, 1)
                        .setUv(glyph.uEnd, glyph.vStart);
                buffer.addVertex(matrix, offset, 0, 0)
                        .setColor(r, g, b, 1)
                        .setUv(glyph.uStart, glyph.vStart);
            }
        }

        if ((style & Terminal.STYLE_UNDERLINE_MASK) != 0) {
            buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT - 3, 0)
                    .setColor(r, g, b, 1)
                    .setUv(0, 0);
            buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT - 3, 0)
                    .setColor(r, g, b, 1)
                    .setUv(0, 0);
            buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT - 2, 0)
                    .setColor(r, g, b, 1)
                    .setUv(0, 0);
            buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT - 2, 0)
                    .setColor(r, g, b, 1)
                    .setUv(0, 0);
        }
    }

    private static FontHandling.FontStyle getFontStyle(byte style) {
        if ((style & Terminal.STYLE_BOLD_MASK) != 0 && (style & Terminal.STYLE_ITALIC_MASK) != 0)
            return FontHandling.FontStyle.BOLD_ITALIC;
        if ((style & Terminal.STYLE_BOLD_MASK) != 0) return FontHandling.FontStyle.BOLD;
        if ((style & Terminal.STYLE_ITALIC_MASK) != 0) return FontHandling.FontStyle.ITALIC;
        return FontHandling.FontStyle.REGULAR;
    }

    public static boolean isPrintableCharacter(final int ch) {
        return ch == 0 || (ch > ' ' && ch <= '~') || ch >= 177;
    }
}