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
                        : (row + terminal.lastRowToDisplay - terminal.height) * terminal.width;
        // Torn mid-resize read (§36 M4): the resize paths swap geometry lock-free from the
        // network thread. Skip rows that don't fit the captured buffer/style/color arrays
        // instead of indexing out of bounds; the remaining per-cell tear is the deferred M4 work.
        final byte[] activeStyles = useAltBuffer ? terminal.altStyles : terminal.styles;
        final int[] activeBuffer = useAltBuffer ? terminal.altBuffer : terminal.buffer;
        final ColorData[] activeColors = useAltBuffer ? terminal.altColors : terminal.colors;
        final ColorData[] activeColorsBackground = useAltBuffer ? terminal.altColorsBackground : terminal.colorsBackground;
        final int end = index + terminal.width;
        if (index < 0 || end > activeStyles.length || end > activeBuffer.length
                || end > activeColors.length || end > activeColorsBackground.length) return;
        for (int col = 0; col < terminal.width; col++, index++) {
            final byte style = activeStyles[index];
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

            final int character = activeBuffer[index];
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

    private static void renderForegroundChar(
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final float offset,
            final int character,
            final int color,
            final byte style) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        if (isBoxDrawingCharacter(character)) {
            renderBoxDrawing(matrix, buffer, offset, character, r, g, b);
        } else if (isPrintableCharacter(character)) {
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

        if (isPrintableCharacter(character) && (style & Terminal.STYLE_CROSSED_OUT_MASK) != 0) {
            // Strikethrough: thickness derived from cell height, centered on midline.
            final float tStrike = Math.max(1f, Terminal.CHAR_HEIGHT / 8f);
            final float cyStrike = Terminal.CHAR_HEIGHT / 2f;
            final float y0 = Math.max(0, cyStrike - tStrike / 2f);
            final float y1 = Math.min(Terminal.CHAR_HEIGHT, cyStrike + tStrike / 2f);
            buffer.addVertex(matrix, offset, y1, 0).setColor(r, g, b, 1).setUv(0, 0);
            buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, y1, 0).setColor(r, g, b, 1).setUv(0, 0);
            buffer.addVertex(matrix, offset + Terminal.CHAR_WIDTH, y0, 0).setColor(r, g, b, 1).setUv(0, 0);
            buffer.addVertex(matrix, offset, y0, 0).setColor(r, g, b, 1).setUv(0, 0);
        }
    }

    private static FontHandling.FontStyle getFontStyle(byte style) {
        if ((style & Terminal.STYLE_BOLD_MASK) != 0 && (style & Terminal.STYLE_ITALIC_MASK) != 0)
            return FontHandling.FontStyle.BOLD_ITALIC;
        if ((style & Terminal.STYLE_BOLD_MASK) != 0) return FontHandling.FontStyle.BOLD;
        if ((style & Terminal.STYLE_ITALIC_MASK) != 0) return FontHandling.FontStyle.ITALIC;
        return FontHandling.FontStyle.REGULAR;
    }

    // Only the line/box-drawing subset of DEC_SPECIAL_GRAPHICS (TerminalBufferWriter) needs
    // vector rendering; the rest (◆ ▒ ° ± π ≤ ≥ ≠ £ · etc.) are real glyphs in the font atlas
    // and fall through to renderForegroundChar's normal glyph path below. Values span two
    // disjoint ranges (0x2500-0x253C box chars, 0x23BA-0x23BD scan-line chars), so each range
    // gets its own primitive bitmask instead of boxing into a Set<Integer> on the hot path.
    private static final long BOX_CHARS_MASK =
            (1L << (0x2500 - 0x2500)) | (1L << (0x2502 - 0x2500)) | (1L << (0x250C - 0x2500))
                    | (1L << (0x2510 - 0x2500)) | (1L << (0x2514 - 0x2500)) | (1L << (0x2518 - 0x2500))
                    | (1L << (0x251C - 0x2500)) | (1L << (0x2524 - 0x2500)) | (1L << (0x252C - 0x2500))
                    | (1L << (0x2534 - 0x2500)) | (1L << (0x253C - 0x2500));
    private static final int SCAN_CHARS_MASK =
            (1 << (0x23BA - 0x23BA)) | (1 << (0x23BB - 0x23BA)) | (1 << (0x23BC - 0x23BA)) | (1 << (0x23BD - 0x23BA));

    private static boolean isBoxDrawingCharacter(final int ch) {
        if (ch >= 0x2500 && ch <= 0x253C) {
            return (BOX_CHARS_MASK & (1L << (ch - 0x2500))) != 0;
        }
        if (ch >= 0x23BA && ch <= 0x23BD) {
            return (SCAN_CHARS_MASK & (1 << (ch - 0x23BA))) != 0;
        }
        return false;
    }

    private static void renderBoxDrawing(final Matrix4f matrix, final BufferBuilder buffer, // NOPMD
            final float offset, final int ch, final float r, final float g, final float b) {
        final float w = Terminal.CHAR_WIDTH;
        final float h = Terminal.CHAR_HEIGHT;
        final float t = 2f; // thickness
        final float cx = w / 2f - t / 2f;
        final float cy = h / 2f - t / 2f;
        // helper to add quad
        // horizontal mid line
        // vertical mid line
        switch (ch) {
            case 0x2500 -> quad(buffer, matrix, offset, cy, offset + w, cy + t, r, g, b); // ─
            case 0x2502 -> quad(buffer, matrix, offset + cx, 0, offset + cx + t, h, r, g, b); // │
            case 0x250C -> { // ┌
                quad(buffer, matrix, offset + cx, cy, offset + w, cy + t, r, g, b);
                quad(buffer, matrix, offset + cx, cy, offset + cx + t, h, r, g, b);
            }
            case 0x2510 -> { // ┐
                quad(buffer, matrix, offset, cy, offset + cx + t, cy + t, r, g, b);
                quad(buffer, matrix, offset + cx, cy, offset + cx + t, h, r, g, b);
            }
            case 0x2514 -> { // └
                quad(buffer, matrix, offset + cx, 0, offset + cx + t, cy + t, r, g, b);
                quad(buffer, matrix, offset + cx, cy, offset + w, cy + t, r, g, b);
            }
            case 0x2518 -> { // ┘
                quad(buffer, matrix, offset + cx, 0, offset + cx + t, cy + t, r, g, b);
                quad(buffer, matrix, offset, cy, offset + cx + t, cy + t, r, g, b);
            }
            case 0x251C -> { // ├
                quad(buffer, matrix, offset + cx, 0, offset + cx + t, h, r, g, b);
                quad(buffer, matrix, offset + cx, cy, offset + w, cy + t, r, g, b);
            }
            case 0x2524 -> { // ┤
                quad(buffer, matrix, offset + cx, 0, offset + cx + t, h, r, g, b);
                quad(buffer, matrix, offset, cy, offset + cx + t, cy + t, r, g, b);
            }
            case 0x252C -> { // ┬
                quad(buffer, matrix, offset, cy, offset + w, cy + t, r, g, b);
                quad(buffer, matrix, offset + cx, cy, offset + cx + t, h, r, g, b);
            }
            case 0x2534 -> { // ┴
                quad(buffer, matrix, offset, cy, offset + w, cy + t, r, g, b);
                quad(buffer, matrix, offset + cx, 0, offset + cx + t, cy + t, r, g, b);
            }
            case 0x253C -> { // ┼
                quad(buffer, matrix, offset, cy, offset + w, cy + t, r, g, b);
                quad(buffer, matrix, offset + cx, 0, offset + cx + t, h, r, g, b);
            }
            case 0x23BA -> quad(buffer, matrix, offset, h * 0.12f, offset + w, h * 0.12f + t, r, g, b); // ⎺ scan 1
            case 0x23BB -> quad(buffer, matrix, offset, h * 0.33f, offset + w, h * 0.33f + t, r, g, b); // ⎻ scan 3
            case 0x23BC -> quad(buffer, matrix, offset, h * 0.66f, offset + w, h * 0.66f + t, r, g, b); // ⎼ scan 7
            case 0x23BD -> quad(buffer, matrix, offset, h * 0.87f, offset + w, h * 0.87f + t, r, g, b); // ⎽ scan 9
            default -> {}
        }
    }

    private static void quad(final BufferBuilder buffer, final Matrix4f matrix,
            final float x0, final float y0, final float x1, final float y1,
            final float r, final float g, final float b) {
        buffer.addVertex(matrix, x0, y1, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x1, y0, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x0, y0, 0).setColor(r, g, b, 1).setUv(0, 0);
    }

    private static boolean isPrintableCharacter(final int ch) {
        return ch == 0 || (ch > ' ' && ch <= '~') || ch >= 177;
    }
}