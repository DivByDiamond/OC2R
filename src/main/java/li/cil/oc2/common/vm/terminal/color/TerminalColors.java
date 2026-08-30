package li.cil.oc2.common.vm.terminal.color;

import com.google.gson.annotations.SerializedName;

public final class TerminalColors {
    // The palette tables below are private immutable defaults: each Terminal holds its own
    // per-instance copy (Terminal.palette256) so OSC 4/104 redefines a color for one terminal
    // only. Kept private so external code can't mutate the shared default (SpotBugs
    // MS_MUTABLE_ARRAY, resolved by design rather than suppression).
    private static final int[] BRIGHT_COLORS = {
        0x555555, 0xFF5555, 0x55FF55, 0xFFFF55,
        0x5555FF, 0xFF55FF, 0x55FFFF, 0xFFFFFF,
    };

    private static final int[] COLORS = {
        0x000000, 0xAA0000, 0x00AA00, 0xAAAA00,
        0x0000AA, 0xAA00AA, 0x00AAAA, 0xAAAAAA,
    };

    // Scale applied to a resolved foreground color when STYLE_DIM (SGR 2, faint) is set. This
    // is a computed modifier layered on AFTER color resolution, matching xterm's computeFaint
    // (util.c) in mechanism — xterm scales the resolved pixel rather than indexing a fixed dim
    // table — so dim composes with bold/blink/256/truecolor instead of overriding them. The 1/2
    // factor reproduces the prior DIM_COLORS table exactly for SIXTEEN_COLOR (0xAA0000 -> 0x550000
    // etc.), so existing dim text is unchanged; what's new is dim now also applies to bright,
    // 256-color, and truecolor cells, which the table silently ignored. DEC VTs (VT100-VT420)
    // have no faint attribute at all — SGR 2 is an ISO/ECMA extension outside real-VT scope, so
    // xterm (the de-facto reference for such extensions) is the guide; the 1/2 scale is a
    // deliberate out-of-box choice, xterm's default is 2/3 (faintIsRelative=false).
    private static final int DIM_FACTOR_NUMERATOR = 1;
    private static final int DIM_FACTOR_DENOMINATOR = 2;

    /**
     * Scale a resolved 0xRRGGBB color for SGR 2 (faint/dim). Applied as a tail modifier in the
     * render path, after the color mode and bold/blink resolution, so dim composes with every
     * other attribute rather than replacing it.
     */
    public static int computeFaint(final int rgb) {
        final int r = ((rgb >> 16) & 0xFF) * DIM_FACTOR_NUMERATOR / DIM_FACTOR_DENOMINATOR;
        final int g = ((rgb >> 8) & 0xFF) * DIM_FACTOR_NUMERATOR / DIM_FACTOR_DENOMINATOR;
        final int b = (rgb & 0xFF) * DIM_FACTOR_NUMERATOR / DIM_FACTOR_DENOMINATOR;
        return (r << 16) | (g << 8) | b;
    }

    private static final int[] COLORS_256 = {
        // 0-7: Normal ANSI colors (must match COLORS)
        0x000000, 0xAA0000, 0x00AA00, 0xAAAA00, 0x0000AA, 0xAA00AA, 0x00AAAA, 0xAAAAAA,
        // 8-15: Bright ANSI colors (must match BRIGHT_COLORS)
        0x555555, 0xFF5555, 0x55FF55, 0xFFFF55, 0x5555FF, 0xFF55FF, 0x55FFFF, 0xFFFFFF,
        0x000000, 0x00005f, 0x000087, 0x0000af, 0x0000d7, 0x0000ff, 0x005f00, 0x005f5f,
        0x005f87, 0x005faf, 0x005fd7, 0x005fff, 0x008700, 0x00875f, 0x008787, 0x0087af,
        0x0087d7, 0x0087ff, 0x00af00, 0x00af5f, 0x00af87, 0x00afaf, 0x00afd7, 0x00afff,
        0x00d700, 0x00d75f, 0x00d787, 0x00d7af, 0x00d7d7, 0x00d7ff, 0x00ff00, 0x00ff5f,
        0x00ff87, 0x00ffaf, 0x00ffd7, 0x00ffff, 0x5f0000, 0x5f005f, 0x5f0087, 0x5f00af,
        0x5f00d7, 0x5f00ff, 0x5f5f00, 0x5f5f5f, 0x5f5f87, 0x5f5faf, 0x5f5fd7, 0x5f5fff,
        0x5f8700, 0x5f875f, 0x5f8787, 0x5f87af, 0x5f87d7, 0x5f87ff, 0x5faf00, 0x5faf5f,
        0x5faf87, 0x5fafaf, 0x5fafd7, 0x5fafff, 0x5fd700, 0x5fd75f, 0x5fd787, 0x5fd7af,
        0x5fd7d7, 0x5fd7ff, 0x5fff00, 0x5fff5f, 0x5fff87, 0x5fffaf, 0x5fffd7, 0x5fffff,
        0x870000, 0x87005f, 0x870087, 0x8700af, 0x8700d7, 0x8700ff, 0x875f00, 0x875f5f,
        0x875f87, 0x875faf, 0x875fd7, 0x875fff, 0x878700, 0x87875f, 0x878787, 0x8787af,
        0x8787d7, 0x8787ff, 0x87af00, 0x87af5f, 0x87af87, 0x87afaf, 0x87afd7, 0x87afff,
        0x87d700, 0x87d75f, 0x87d787, 0x87d7af, 0x87d7d7, 0x87d7ff, 0x87ff00, 0x87ff5f,
        0x87ff87, 0x87ffaf, 0x87ffd7, 0x87ffff, 0xaf0000, 0xaf005f, 0xaf0087, 0xaf00af,
        0xaf00d7, 0xaf00ff, 0xaf5f00, 0xaf5f5f, 0xaf5f87, 0xaf5faf, 0xaf5fd7, 0xaf5fff,
        0xaf8700, 0xaf875f, 0xaf8787, 0xaf87af, 0xaf87d7, 0xaf87ff, 0xafaf00, 0xafaf5f,
        0xafaf87, 0xafafaf, 0xafafd7, 0xafafff, 0xafd700, 0xafd75f, 0xafd787, 0xafd7af,
        0xafd7d7, 0xafd7ff, 0xafff00, 0xafff5f, 0xafff87, 0xafffaf, 0xafffd7, 0xafffff,
        0xd70000, 0xd7005f, 0xd70087, 0xd700af, 0xd700d7, 0xd700ff, 0xd75f00, 0xd75f5f,
        0xd75f87, 0xd75faf, 0xd75fd7, 0xd75fff, 0xd78700, 0xd7875f, 0xd78787, 0xd787af,
        0xd787d7, 0xd787ff, 0xd7af00, 0xd7af5f, 0xd7af87, 0xd7afaf, 0xd7afd7, 0xd7afff,
        0xd7d700, 0xd7d75f, 0xd7d787, 0xd7d7af, 0xd7d7d7, 0xd7d7ff, 0xd7ff00, 0xd7ff5f,
        0xd7ff87, 0xd7ffaf, 0xd7ffd7, 0xd7ffff, 0xff0000, 0xff005f, 0xff0087, 0xff00af,
        0xff00d7, 0xff00ff, 0xff5f00, 0xff5f5f, 0xff5f87, 0xff5faf, 0xff5fd7, 0xff5fff,
        0xff8700, 0xff875f, 0xff8787, 0xff87af, 0xff87d7, 0xff87ff, 0xffaf00, 0xffaf5f,
        0xffaf87, 0xffafaf, 0xffafd7, 0xffafff, 0xffd700, 0xffd75f, 0xffd787, 0xffd7af,
        0xffd7d7, 0xffd7ff, 0xffff00, 0xffff5f, 0xffff87, 0xffffaf, 0xffffd7, 0xffffff,
        0x080808, 0x121212, 0x1c1c1c, 0x262626, 0x303030, 0x3a3a3a, 0x444444, 0x4e4e4e,
        0x585858, 0x626262, 0x6c6c6c, 0x767676, 0x808080, 0x8a8a8a, 0x949494, 0x9e9e9e,
        0xa8a8a8, 0xb2b2b2, 0xbcbcbc, 0xc6c6c6, 0xd0d0d0, 0xdadada, 0xe4e4e4, 0xeeeeee
    };

    // Fresh copy of the default 256-color palette. Each Terminal initializes its own
    // per-instance palette256 from this (so OSC 4 redefines a color for one terminal
    // only) and resets it via OSC 104.
    public static int[] getDefaultPalette256() {
        return COLORS_256.clone();
    }

    // Fresh copies of the default ANSI-16 tables. The 256-color palette is canonical —
    // COLORS_256[0..7] == COLORS, [8..15] == BRIGHT_COLORS — so these exist for tests that
    // cross-check the 16 against the 256, and for any code reading the base-16 defaults
    // without a Terminal instance. Each returns a defensive clone (the arrays are private).
    public static int[] getDefaultColors16() {
        return COLORS.clone();
    }

    public static int[] getDefaultBrightColors16() {
        return BRIGHT_COLORS.clone();
    }

    // Fixed defaults for the DEFAULT_FOREGROUND/BACKGROUND render modes. These must NOT
    // track OSC 4 — in xterm the default fg/bg are addressed by OSC 10/11, not OSC 4 — so
    // they read the immutable defaults directly, never the instance palette.
    public static int defaultForegroundRgb(final boolean bold) {
        return bold ? BRIGHT_COLORS[Color.WHITE] : COLORS[Color.WHITE];
    }

    public static int defaultBackgroundRgb() {
        return 0x000000;
    }

    public static final ColorData DEFAULT_BACKGROUND_COLOR =
            new ColorData(Color.WHITE, Color.BLACK, 0, ColorMode.DEFAULT_BACKGROUND);
    public static final ColorData DEFAULT_FOREGROUND_COLOR =
            new ColorData(Color.WHITE, Color.BLACK, 0, ColorMode.DEFAULT_FOREGROUND);
    public static final ColorData DEFAULT_BRIGHT_COLORS =
            new ColorData(Color.WHITE, Color.BLACK, 0, ColorMode.SIXTEEN_COLOR_BRIGHT);
    public static final ColorData DEFAULT_COLORS =
            new ColorData(Color.WHITE, Color.BLACK, 0, ColorMode.SIXTEEN_COLOR);
    public static final byte DEFAULT_STYLE = 0;
    public static final ColorData DEFAULT_256_COLORS =
            new ColorData(Color.WHITE, Color.BLACK, 0, ColorMode.TWO_FIFTY_SIX_COLOR);
    public static final ColorData DEFAULT_TRUE_COLOR_FOREGROUND =
            new ColorData(238, 238, 238, ColorMode.TRUE_COLOR);
    public static final ColorData DEFAULT_TRUE_COLOR_BACKGROUND =
            new ColorData(0, 0, 0, ColorMode.TRUE_COLOR);
    public static final int TAB_WIDTH = 8;

    public enum ColorMode {
        @SerializedName("0")
        SIXTEEN_COLOR,
        @SerializedName("1")
        TWO_FIFTY_SIX_COLOR,
        @SerializedName("2")
        TRUE_COLOR,
        @SerializedName("3")
        SIXTEEN_COLOR_BRIGHT,
        @SerializedName("4")
        DEFAULT_BACKGROUND,
        @SerializedName("5")
        DEFAULT_FOREGROUND,
    }

    public static final class CursorMode {
        public static final int DEFAULT = 0;
        public static final int BLINK_BLOCK = 1;
        public static final int STEADY_BLOCK = 2;
        public static final int BLINK_UNDERLINE = 3;
        public static final int STEADY_UNDERLINE = 4;
        public static final int BLINKING_BAR_LINE = 5;
        public static final int STEADY_BAR_LINE = 6;
    }

    public static final class DrawingMode {
        public static final int ASCII = 0;
        public static final int SPECIAL_GRAPHICS = 1;
    }

    @SuppressWarnings("unused")
    public static final class Color {
        public static final int BLACK = 0;
        public static final int RED = 1;
        public static final int GREEN = 2;
        public static final int YELLOW = 3;
        public static final int BLUE = 4;
        public static final int MAGENTA = 5;
        public static final int CYAN = 6;
        public static final int WHITE = 7;
    }

    public static class ColorData {
        public int r;
        public int g;
        public int b;
        public ColorMode mode;

        @SuppressWarnings("unused")
        public ColorData() {
            r = 0;
            g = 0;
            b = 0;
            mode = ColorMode.SIXTEEN_COLOR;
        }

        public ColorData(final int r, final int g, final int b, final ColorMode mode) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.mode = mode;
        }

        public int toInt() {
            return (r & 0b11111111) << 16 | (g & 0b11111111) << 8 | (b & 0b11111111);
        }

        public ColorData copy() {
            return new ColorData(r, g, b, mode);
        }
    }
}