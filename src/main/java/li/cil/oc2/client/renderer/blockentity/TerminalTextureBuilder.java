package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.platform.NativeImage;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalCharRenderer;
import li.cil.oc2.common.vm.terminal.TerminalColors;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.TerminalColors.ColorMode;
import li.cil.oc2.common.vm.terminal.fonts.FontHandling;
import li.cil.oc2.common.vm.terminal.fonts.Glyph;

/**
 * Renders terminal characters, backgrounds, and cursor into a NativeImage.
 */
final class TerminalTextureBuilder {
    static final int TEX_W = Terminal.WIDTH * Terminal.CHAR_WIDTH;   // 640
    static final int TEX_H = Terminal.HEIGHT * Terminal.CHAR_HEIGHT; // 384

    private TerminalTextureBuilder() {}

    static void updateTexture(final NativeImage img, final Terminal terminal) {
        // Clear entire texture to opaque black
        fillRect(img, 0, 0, TEX_W, TEX_H, 0xFF000000);

        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        final int[]   chars = alt ? terminal.altBuffer           : terminal.buffer;
        final ColorData[] fg = alt ? terminal.altColors           : terminal.colors;
        final ColorData[] bg = alt ? terminal.altColorsBackground : terminal.colorsBackground;
        final byte[]  st    = alt ? terminal.altStyles            : terminal.styles;
        final int     si    = alt ? 0
                : (terminal.lastRowToDisplay - Terminal.HEIGHT) * Terminal.WIDTH;

        for (int row = 0; row < Terminal.HEIGHT; row++) {
            for (int col = 0; col < Terminal.WIDTH; col++) {
                final int i = si + row * Terminal.WIDTH + col;
                final byte style = st[i];
                if ((style & Terminal.STYLE_HIDDEN_MASK) != 0) continue;

                final boolean inv = (style & Terminal.STYLE_INVERT_MASK) != 0;
                final int fgC = resolveColor(inv ? bg[i] : fg[i], style, !inv);
                final int bgC = resolveColor(inv ? fg[i] : bg[i], style,  inv);

                final int px = col * Terminal.CHAR_WIDTH;
                final int py = row * Terminal.CHAR_HEIGHT;
                fillRect(img, px, py, Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT, toNative(bgC));
                drawChar(img, px, py, chars[i], toNative(fgC), style);
            }
        }

        drawCursor(img, terminal);
    }

    // ── Color resolution (matches TerminalCharRenderer / TerminalBackgroundRenderer) ──

    private static int resolveColor(final ColorData cd, final byte style, final boolean useR) {
        if (cd.Mode == ColorMode.TRUE_COLOR)        return cd.ToInt();
        if (cd.Mode == ColorMode.DEFAULT_BACKGROUND) return 0x000000;
        final int idx = useR ? cd.R : cd.G;
        final int[] pal = (style & Terminal.STYLE_DIM_MASK) != 0
                ? TerminalColors.DIM_COLORS : TerminalColors.COLORS;
        return switch (cd.Mode) {
            case SIXTEEN_COLOR       -> pal[idx];
            case SIXTEEN_COLOR_BRIGHT -> TerminalColors.BRIGHT_COLORS[idx];
            case TWO_FIFTY_SIX_COLOR  -> TerminalColors.COLORS_256[idx];
            default                   -> 0x000000;
        };
    }

    // ── Character drawing into NativeImage ──────────────────────────────────

    private static void drawChar(
            final NativeImage img, int px, int py,
            final int ch, final int nativeFg, final byte style) {
        // Underline first (drawn even for non-printable chars)
        if ((style & Terminal.STYLE_UNDERLINE_MASK) != 0) {
            for (int x = 0; x < Terminal.CHAR_WIDTH; x++) {
                img.setPixelRGBA(px + x, py + Terminal.CHAR_HEIGHT - 3, nativeFg);
                img.setPixelRGBA(px + x, py + Terminal.CHAR_HEIGHT - 2, nativeFg);
            }
        }

        if (!TerminalCharRenderer.isPrintableCharacter(ch) || ch == 0) return;

        final FontHandling.FontStyle fs = getFontStyle(style);
        final Glyph glyph = FontHandling.getGlyph(ch, fs);
        final float sx = (float) glyph.width / Terminal.CHAR_WIDTH;
        final float sy = (float) glyph.height / Terminal.CHAR_HEIGHT;

        for (int y = 0; y < Terminal.CHAR_HEIGHT; y++) {
            for (int x = 0; x < Terminal.CHAR_WIDTH; x++) {
                final int gx = Math.min((int) (x * sx), glyph.width  - 1);
                final int gy = Math.min((int) (y * sy), glyph.height - 1);
                final int pixel = glyph.image.getRGB(gx, gy);
                if (((pixel >> 24) & 0xFF) > 64) {
                    img.setPixelRGBA(px + x, py + y, nativeFg);
                }
            }
        }
    }

    // ── Cursor rendering ────────────────────────────────────────────────────

    private static void drawCursor(final NativeImage img, final Terminal terminal) {
        if (!terminal.currentPrivateModeState.DECTCEM) return;
        final int cx = terminal.x, cy = terminal.y;
        if (cx < 0 || cx >= Terminal.WIDTH || cy < 0 || cy >= Terminal.HEIGHT) return;
        final int px = cx * Terminal.CHAR_WIDTH;
        final int py = cy * Terminal.CHAR_HEIGHT;
        final int cc = toNative(0xEEEEEE);
        switch (terminal.cursorMode) {
            case TerminalColors.CursorMode.DEFAULT,
                 TerminalColors.CursorMode.BLINK_BLOCK,
                 TerminalColors.CursorMode.STEADY_BLOCK ->
                fillRect(img, px, py, Terminal.CHAR_WIDTH, Terminal.CHAR_HEIGHT, cc);
            case TerminalColors.CursorMode.BLINK_UNDERLINE,
                 TerminalColors.CursorMode.STEADY_UNDERLINE ->
                fillRect(img, px, py + Terminal.CHAR_HEIGHT - 2, Terminal.CHAR_WIDTH, 2, cc);
            case TerminalColors.CursorMode.BLINKING_BAR_LINE,
                 TerminalColors.CursorMode.STEADY_BAR_LINE ->
                fillRect(img, px, py, 2, Terminal.CHAR_HEIGHT, cc);
            default -> {}
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void fillRect(
            final NativeImage img, final int x, final int y,
            final int w, final int h, final int nativeColor) {
        for (int dy = 0; dy < h; dy++)
            for (int dx = 0; dx < w; dx++)
                img.setPixelRGBA(x + dx, y + dy, nativeColor);
    }

    /** Convert 0xRRGGBB color to NativeImage pixel format (RGBA with A=FF). */
    private static int toNative(final int rgb) {
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8)  & 0xFF;
        final int b = rgb & 0xFF;
        return r | (g << 8) | (b << 16) | 0xFF000000;
    }

    private static FontHandling.FontStyle getFontStyle(final byte style) {
        if ((style & Terminal.STYLE_BOLD_MASK) != 0
                && (style & Terminal.STYLE_ITALIC_MASK) != 0)
            return FontHandling.FontStyle.BOLD_ITALIC;
        if ((style & Terminal.STYLE_BOLD_MASK) != 0)  return FontHandling.FontStyle.BOLD;
        if ((style & Terminal.STYLE_ITALIC_MASK) != 0) return FontHandling.FontStyle.ITALIC;
        return FontHandling.FontStyle.REGULAR;
    }
}
