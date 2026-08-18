package li.cil.oc2.common.vm.terminal.color;

import com.google.gson.annotations.SerializedName;

public final class TerminalColors {
    public static final int[] BRIGHT_COLORS = {
        0x555555, 0xFF5555, 0x55FF55, 0xFFFF55,
        0x5555FF, 0xFF55FF, 0x55FFFF, 0xFFFFFF,
    };

    public static final int[] COLORS = {
        0x000000, 0xAA0000, 0x00AA00, 0xAAAA00,
        0x0000AA, 0xAA00AA, 0x00AAAA, 0xAAAAAA,
    };

    public static final int[] DIM_COLORS = {
        0x000000, 0x550000, 0x005500, 0x555500,
        0x000055, 0x550055, 0x005555, 0x555555,
    };

    public static final int[] COLORS_256 = {
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
        0xd787d7, 0xd787ff, 0xdfaf00, 0xdfaf5f, 0xdfaf87, 0xdfafaf, 0xdfafdf, 0xdfafff,
        0xdfdf00, 0xdfdf5f, 0xdfdf87, 0xdfdfaf, 0xdfdfdf, 0xdfdfff, 0xdfff00, 0xdfff5f,
        0xdfff87, 0xdfffaf, 0xdfffdf, 0xdfffff, 0xff0000, 0xff005f, 0xff0087, 0xff00af,
        0xff00df, 0xff00ff, 0xff5f00, 0xff5f5f, 0xff5f87, 0xff5faf, 0xff5fdf, 0xff5fff,
        0xff8700, 0xff875f, 0xff8787, 0xff87af, 0xff87df, 0xff87ff, 0xffaf00, 0xffaf5f,
        0xffaf87, 0xffafaf, 0xffafdf, 0xffafff, 0xffdf00, 0xffdf5f, 0xffdf87, 0xffdfaf,
        0xffdfdf, 0xffdfff, 0xffff00, 0xffff5f, 0xffff87, 0xffffaf, 0xffffdf, 0xffffff,
        0x080808, 0x121212, 0x1c1c1c, 0x262626, 0x303030, 0x3a3a3a, 0x444444, 0x4e4e4e,
        0x585858, 0x626262, 0x6c6c6c, 0x767676, 0x808080, 0x8a8a8a, 0x949494, 0x9e9e9e,
        0xa8a8a8, 0xb2b2b2, 0xbcbcbc, 0xc6c6c6, 0xd0d0d0, 0xdadada, 0xe4e4e4, 0xeeeeee
    };

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
        public int R;
        public int G;
        public int B;
        public ColorMode Mode;

        @SuppressWarnings("unused")
        public ColorData() {
            R = 0;
            G = 0;
            B = 0;
            Mode = ColorMode.SIXTEEN_COLOR;
        }

        public ColorData(final int r, final int g, final int b, final ColorMode mode) {
            R = r;
            G = g;
            B = b;
            Mode = mode;
        }

        public int ToInt() {
            return (((R & 0b11111111) << 16) | ((G & 0b11111111) << 8) | (B & 0b11111111));
        }

        public ColorData Copy() {
            return new ColorData(R, G, B, Mode);
        }
    }
}