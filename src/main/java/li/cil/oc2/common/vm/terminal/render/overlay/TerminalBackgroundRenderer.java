package li.cil.oc2.common.vm.terminal.render.overlay;

import com.mojang.blaze3d.vertex.BufferBuilder;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TerminalBackgroundRenderer {
    public static void renderBackground(
            final Terminal terminal,
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final int row) {
        float backgroundStartX = -1;
        int backgroundColor = 0;
        float tx = 0f;
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        int index =
                useAltBuffer
                        ? row * Terminal.WIDTH
                        : (row + (terminal.lastRowToDisplay - Terminal.HEIGHT)) * Terminal.WIDTH;
        for (int col = 0; col < Terminal.WIDTH; col++, index++) {
            final byte style = useAltBuffer ? terminal.altStyles[index] : terminal.styles[index];
            final boolean screenInverted = terminal.currentPrivateModeState.DECSCNM;
            final boolean invertBackground = ((style & Terminal.STYLE_INVERT_MASK) != 0) ^ screenInverted;
            final ColorData color =
                    !invertBackground
                            ? useAltBuffer
                                    ? terminal.altColorsBackground[index]
                                    : terminal.colorsBackground[index]
                            : useAltBuffer ? terminal.altColors[index] : terminal.colors[index];

            if ((style & Terminal.STYLE_HIDDEN_MASK) != 0) continue;

            // --- Blink semantics (VT100) ---
            // invertBackground determines WHAT blinks:
            //   false → foreground blinks (background always renders)
            //   true  → background blinks (foreground always renders)
            // Bold changes blink from on/off to normal/bright intensity.
            final boolean isBold = (style & Terminal.STYLE_BOLD_MASK) != 0;
            final boolean isBlinking = (style & Terminal.STYLE_BLINK_MASK) != 0;
            final boolean blinkOff = isBlinking
                    && (System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500;

            // Background blinks only when inverted.
            //   Not bold: no background during off phase (on/off blink)
            //   Bold: normal palette during off phase, bright during on phase
            final boolean suppressBgForBlink = isBlinking && invertBackground && blinkOff && !isBold;
            final boolean dimBoldBgForBlink = isBlinking && invertBackground && blinkOff && isBold;

            // Palette selection per color mode (must match TerminalCharRenderer):
            //   DEFAULT_FOREGROUND: bold → bright, else normal. Dim doesn't affect background.
            //   SIXTEEN_COLOR: always COLORS (bold = typeface only, not color change).
            //   SIXTEEN_COLOR_BRIGHT: always BRIGHT_COLORS.
            int background =
                    switch (color.Mode) {
                        case SIXTEEN_COLOR ->
                                TerminalColors.COLORS[!invertBackground ? color.G : color.R];
                        case TWO_FIFTY_SIX_COLOR ->
                                TerminalColors.COLORS_256[!invertBackground ? color.G : color.R];
                        case TRUE_COLOR -> color.ToInt();
                        case SIXTEEN_COLOR_BRIGHT ->
                                (dimBoldBgForBlink ? TerminalColors.COLORS : TerminalColors.BRIGHT_COLORS)
                                        [!invertBackground ? color.G : color.R];
                        case DEFAULT_BACKGROUND -> 0x000000;
                        case DEFAULT_FOREGROUND ->
                                ((isBold && !dimBoldBgForBlink) ? TerminalColors.BRIGHT_COLORS
                                        : TerminalColors.COLORS)[TerminalColors.Color.WHITE];
                        default -> throw new AssertionError(color.Mode);
                    };

            // When background is blinking (inverted + blink), suppress bg on off phase (non-bold only)
            if (suppressBgForBlink) {
                background = 0x000000;
            }

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

    static void renderBackgroundRect(
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final float x0,
            final float x1,
            final int color) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        buffer.addVertex(matrix, x0, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x1, Terminal.CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x1, 0, 0).setColor(r, g, b, 1).setUv(0, 0);
        buffer.addVertex(matrix, x0, 0, 0).setColor(r, g, b, 1).setUv(0, 0);
    }
}