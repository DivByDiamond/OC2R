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
    public static void renderBackground(final Terminal terminal, // NOPMD: data-driven background render (DECSCNM, blink)
            final Matrix4f matrix,
            final BufferBuilder buffer,
            final int row) {
        final BackgroundRun run = new BackgroundRun();
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
            final ColorData color = resolveColor(terminal, useAltBuffer, index, invertBackground);
            int background = resolveBackground(terminal, color, invertBackground, isBold, isBlinking, blinkOff);
            // When the background blinks (inverted + blink), suppress it on the off phase
            // (non-bold only — bold blink alternates normal/bright instead).
            if (isBlinking && invertBackground && blinkOff && !isBold) {
                background = 0x000000;
            }

            run.advance(tx, background, matrix, buffer);
            tx += Terminal.CHAR_WIDTH;
        }

        if (run.active()) {
            renderBackgroundRect(matrix, buffer, run.startX, tx, run.color);
        }
    }

    private static ColorData resolveColor(
            final Terminal terminal,
            final boolean useAltBuffer,
            final int index,
            final boolean invertBackground) {
        return !invertBackground
                ? useAltBuffer
                        ? terminal.altColorsBackground[index]
                        : terminal.colorsBackground[index]
                : useAltBuffer ? terminal.altColors[index] : terminal.colors[index];
    }

    private static int resolveBackground(final Terminal terminal, // NOPMD: data-driven color-mode switch (bold-bright, blink)
            final ColorData color,
            final boolean invertBackground,
            final boolean isBold, final boolean isBlinking, final boolean blinkOff) {
        // Bold blink alternates normal/bright intensity instead of on/off.
        final boolean dimBoldForBlink = isBlinking && invertBackground && blinkOff && isBold;
        final int channel = backgroundChannel(color, invertBackground);
        // Note: SGR 2 (faint/dim) is NOT applied to the background — xterm's getXtermBackground
        // (util.c) consumes ATR_FAINT only in the foreground path. The prior code dimmed the
        // SIXTEEN_COLOR background, a divergence; dropped here to match xterm.
        return switch (color.Mode) {
            case SIXTEEN_COLOR -> terminal.palette256[channel];
            case TWO_FIFTY_SIX_COLOR -> terminal.palette256[channel];
            case TRUE_COLOR -> color.toInt();
            // Bright ANSI (8-15) live at palette256[8..15]; dimBoldForBlink drops back to normal.
            case SIXTEEN_COLOR_BRIGHT ->
                    terminal.palette256[channel + (dimBoldForBlink ? 0 : 8)];
            case DEFAULT_BACKGROUND -> TerminalColors.defaultBackgroundRgb();
            // DEFAULT_FOREGROUND must not track OSC 4 (xterm reserves it for OSC 10/11).
            case DEFAULT_FOREGROUND -> TerminalColors.defaultForegroundRgb(isBold && !dimBoldForBlink);
            default -> throw new AssertionError(color.Mode);
        };
    }

    private static int backgroundChannel(final ColorData color, final boolean invertBackground) {
        return invertBackground ? color.R : color.G;
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

    private static final class BackgroundRun {
        float startX = -1;
        int color = 0;

        void advance(
                final float tx,
                final int background,
                final Matrix4f matrix,
                final BufferBuilder buffer) {
            final boolean hadBackground = startX >= 0;
            final boolean hasBackground = background != 0x000000;
            if (!hadBackground && hasBackground) {
                startX = tx;
                color = background;
            } else if (hadBackground && (!hasBackground || color != background)) {
                renderBackgroundRect(matrix, buffer, startX, tx, color);
                if (hasBackground) {
                    startX = tx;
                    color = background;
                } else {
                    startX = -1;
                }
            }
        }

        boolean active() {
            return startX >= 0;
        }
    }
}