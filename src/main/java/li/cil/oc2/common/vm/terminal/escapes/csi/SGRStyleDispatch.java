package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;

/**
 * Dispatches SGR (Select Graphic Rendition) codes to terminal state changes.
 * Each SGR code maps to a single style or color mutation.
 */
public final class SGRStyleDispatch {

    private SGRStyleDispatch() {}

    /**
     * Apply a single SGR code to the terminal.
     * Handles style flags (bold, dim, italic, underline, blink, invert, hidden)
     * and standard color codes (30-37, 40-47, 90-97, 100-107, 39, 49).
     * Does NOT handle extended color (38/48) — those are parsed by SGRColorParser.
     *
     * @param terminal The terminal to mutate.
     * @param code     The SGR code to apply.
     */
    public static void apply(final Terminal terminal, final int code) { // NOPMD: inherently large data-driven ANSI SGR style switch
        switch (code) {
            case 0 -> { // Reset / Normal
                terminal.sixteenColor = TerminalColors.DEFAULT_COLORS.copy();
                terminal.sixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
                terminal.style = TerminalColors.DEFAULT_STYLE;
                terminal.currentForegroundColorMode = TerminalColors.ColorMode.DEFAULT_FOREGROUND;
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
                terminal.twoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
                terminal.foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
                terminal.backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
            }
            case 1 -> // Bold or increased intensity
                    terminal.style |= Terminal.STYLE_BOLD_MASK;
            case 2 -> // Faint or decreased intensity
                    terminal.style |= Terminal.STYLE_DIM_MASK;
            case 3 -> terminal.style |= Terminal.STYLE_ITALIC_MASK;
            case 4 -> // Underscore
                    terminal.style |= Terminal.STYLE_UNDERLINE_MASK;
            case 5 -> // Blink
                    terminal.style |= Terminal.STYLE_BLINK_MASK;
            case 7 -> // Negative (reverse) image
                    terminal.style |= Terminal.STYLE_INVERT_MASK;
            case 8 -> // Conceal aka Hide
                    terminal.style |= Terminal.STYLE_HIDDEN_MASK;
            case 22 -> // Normal color or intensity
                    terminal.style &= ~(Terminal.STYLE_BOLD_MASK | Terminal.STYLE_DIM_MASK);
            case 23 -> terminal.style &= ~Terminal.STYLE_ITALIC_MASK;
            case 24 -> // Underline off
                    terminal.style &= ~Terminal.STYLE_UNDERLINE_MASK;
            case 25 -> // Blink off
                    terminal.style &= ~Terminal.STYLE_BLINK_MASK;
            case 27 -> // Reverse/invert off
                    terminal.style &= ~Terminal.STYLE_INVERT_MASK;
            case 28 -> // Reveal conceal off
                    terminal.style &= ~Terminal.STYLE_HIDDEN_MASK;
            case 30, 31, 32, 33, 34, 35, 36, 37 -> { // Set foreground color
                terminal.currentForegroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
                terminal.sixteenColor.r = code - 30;
            }
            case 39 -> { // Default foreground color
                terminal.currentForegroundColorMode = TerminalColors.ColorMode.DEFAULT_FOREGROUND;
                terminal.foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
                terminal.sixteenColor.r = TerminalColors.Color.WHITE;
                terminal.sixteenColorBright.r = TerminalColors.Color.WHITE;
            }
            case 40, 41, 42, 43, 44, 45, 46, 47 -> { // Set background color
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
                terminal.sixteenColor.g = code - 40;
            }
            case 49 -> { // Default background color
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
                terminal.backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
                terminal.sixteenColor.g = TerminalColors.Color.BLACK;
                terminal.sixteenColorBright.g = TerminalColors.Color.BLACK;
            }
            case 90, 91, 92, 93, 94, 95, 96, 97 -> { // Set foreground color
                terminal.currentForegroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR_BRIGHT;
                terminal.sixteenColorBright.r = code - 90;
            }
            case 100, 101, 102, 103, 104, 105, 106, 107 -> { // Set background color
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR_BRIGHT;
                terminal.sixteenColorBright.g = code - 100;
            }
            default -> {}
        }
    }
}
