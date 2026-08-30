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
    static void renderForeground(final Terminal terminal, // NOPMD: data-driven render loop (DECSCNM inverse, VT100 blink)
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final int row) {
        float tx = 0f;
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        int index =
                useAltBuffer
                        ? row * terminal.width
                        : (row + terminal.lastRowToDisplay - Terminal.HEIGHT) * terminal.width;
        for (int col = 0; col < terminal.width; col++, index++) {
            final byte style = useAltBuffer ? terminal.altStyles[index] : terminal.styles[index];
            if ((style & Terminal.STYLE_HIDDEN_MASK) != 0) continue;

            // DECSCNM screen inverse: XOR the per-cell SGR 7 invert with the screen-inverse mode.
            final boolean screenInverted = terminal.currentPrivateModeState.DECSCNM;
            final boolean invertBackground = ((style & Terminal.STYLE_INVERT_MASK) != 0) ^ screenInverted;
            final boolean isBold = (style & Terminal.STYLE_BOLD_MASK) != 0;
            final boolean isBlinking = (style & Terminal.STYLE_BLINK_MASK) != 0;
            final boolean blinkOff = isBlinking
                    && Math.floorMod(System.currentTimeMillis() + terminal.hashCode(), 1000) > 500;
            // VT100 blink: non-bold, non-inverted blink chars disappear on the off phase;
            // bold blink alternates normal/bright intensity instead (handled below).
            // For inverted (SGR 7 / DECSCNM) blink cells the glyph stays visible and the
            // background blinks instead (see TerminalBackgroundRenderer) — xterm semantics
            // where blink affects the background of reverse-video text.
            if (isBlinking && !invertBackground && blinkOff && !isBold) {
                tx += Terminal.CHAR_WIDTH;
                continue;
            }

            final int character = useAltBuffer ? terminal.altBuffer[index] : terminal.buffer[index];
            final int foreground =
                    getForegroundColor(terminal, style, index, useAltBuffer, invertBackground, isBold, isBlinking, blinkOff);
            renderForegroundChar(matrix, buffer, tx, character, foreground, style);
            tx += Terminal.CHAR_WIDTH;
        }
    }

    private static int getForegroundColor(final Terminal terminal, // NOPMD: data-driven color-mode switch (boldIsBright, VT100 blink)
            final byte style, final int index, final boolean useAltBuffer,
            final boolean invertBackground, final boolean isBold, final boolean isBlinking, final boolean blinkOff) {
        final ColorData color = selectColor(terminal, index, useAltBuffer, invertBackground);
        final boolean isDim = (style & Terminal.STYLE_DIM_MASK) != 0;
        // Bold blink alternates normal/bright intensity instead of on/off.
        final boolean dimBoldForBlink = isBlinking && !invertBackground && blinkOff && isBold;
        final int channel = foregroundChannel(color, invertBackground);
        final int rgb = switch (color.mode) {
            // DEFAULT_FOREGROUND must not track OSC 4 (xterm reserves it for OSC 10/11).
            case DEFAULT_FOREGROUND -> TerminalColors.defaultForegroundRgb(isBold && !dimBoldForBlink);
            case SIXTEEN_COLOR -> terminal.palette256[channel];
            case TWO_FIFTY_SIX_COLOR -> terminal.palette256[channel];
            case TRUE_COLOR -> color.toInt();
            // Bright ANSI (8-15) live at palette256[8..15]; dimBoldForBlink drops back to normal.
            case SIXTEEN_COLOR_BRIGHT ->
                    terminal.palette256[channel + (dimBoldForBlink ? 0 : 8)];
            case DEFAULT_BACKGROUND -> TerminalColors.defaultBackgroundRgb();
            default -> throw new AssertionError(color.mode);
        };
        // Dim (SGR 2) is a tail modifier on the resolved color — composes with bold/blink and
        // now applies to every mode (the old fixed DIM_COLORS table only covered SIXTEEN_COLOR).
        return isDim ? TerminalColors.computeFaint(rgb) : rgb;
    }

    private static ColorData selectColor(
            final Terminal terminal,
            final int index,
            final boolean useAltBuffer,
            final boolean invertBackground) {
        return !invertBackground
                ? useAltBuffer ? terminal.altColors[index] : terminal.colors[index]
                : useAltBuffer
                        ? terminal.altColorsBackground[index]
                        : terminal.colorsBackground[index];
    }

    private static int foregroundChannel(final ColorData color, final boolean invertBackground) {
        return invertBackground ? color.g : color.r;
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
            // Solid color quad at the bottom of the cell. UV (0,0) samples the opaque white
            // reference square in the font atlas — POSITION_TEX_COLOR needs a valid texel, but the
            // color comes from the vertex color. Vertex order: BL -> BR -> TR -> TL (CCW).
            buffer.addVertex(matrix, offset, Terminal.CHAR_HEIGHT, 0)
                    .setColor(r, g, b, 1)
                    .setUv(0, 0);
            buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT, 0)
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