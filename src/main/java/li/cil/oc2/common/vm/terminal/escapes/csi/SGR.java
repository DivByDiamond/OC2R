package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;

public class SGR extends CSISequenceHandler {
    public SGR(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {0};
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        int max = Math.max(1, argCount);
        int i = 0;
        while (i < max) {
            int v1 = args[i];
            if (v1 == 38 || v1 == 48) {
                int index = i + 1;
                if (index >= max) {
                    i++;
                    continue;
                }
                // Skip consumed sub-args. If nothing matched, still skip v2
                // so it isn't re-read as a top-level SGR code.
                i = handleExtendedColor(terminal, v1, args, index, argCount) + 1;
                continue;
            }

            selectStyle(terminal, v1);
            i++;
        }
    }

    private static int handleExtendedColor(
            Terminal terminal, int v1, int[] args, int index, int argCount) {
        int idx = index;
        int v2 = args[idx];
        return v1 == 38
                ? handleForegroundColor(terminal, v2, args, idx, argCount)
                : handleBackgroundColor(terminal, v2, args, idx, argCount);
    }

    private static int handleForegroundColor(
            Terminal terminal, int v2, int[] args, int index, int argCount) {
        int idx = index;
        if (v2 == 5 && idx + 1 < argCount) {
            terminal.currentForegroundColorMode =
                    TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR;
            terminal.twoFiftySixColor.R = args[++idx];
        } else if (v2 == 2 && idx + 3 < argCount) {
            terminal.currentForegroundColorMode = TerminalColors.ColorMode.TRUE_COLOR;
            terminal.foregroundColor =
                    new TerminalColors.ColorData(
                            args[++idx],
                            args[++idx],
                            args[++idx],
                            TerminalColors.ColorMode.TRUE_COLOR);
        }
        return idx;
    }

    private static int handleBackgroundColor(
            Terminal terminal, int v2, int[] args, int index, int argCount) {
        int idx = index;
        if (v2 == 5 && idx + 1 < argCount) {
            terminal.currentBackgroundColorMode =
                    TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR;
            terminal.twoFiftySixColor.G = args[++idx];
        } else if (v2 == 2 && idx + 3 < argCount) {
            terminal.currentBackgroundColorMode = TerminalColors.ColorMode.TRUE_COLOR;
            terminal.backgroundColor =
                    new TerminalColors.ColorData(
                            args[++idx],
                            args[++idx],
                            args[++idx],
                            TerminalColors.ColorMode.TRUE_COLOR);
        }
        return idx;
    }

    private static void selectStyle(Terminal terminal, int arg) { // NOPMD: inherently large data-driven ANSI SGR style switch
        switch (arg) {
            case 0 -> { // Reset / Normal
                terminal.sixteenColor = TerminalColors.DEFAULT_COLORS.copy();
                terminal.sixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
                terminal.style = TerminalColors.DEFAULT_STYLE;
                terminal.currentForegroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
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
                terminal.sixteenColor.R = arg - 30;
            }
            case 39 -> { // Default foreground color
                terminal.currentForegroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
                terminal.foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
                terminal.sixteenColor.R = TerminalColors.Color.WHITE;
            }
            case 40, 41, 42, 43, 44, 45, 46, 47 -> { // Set background color
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
                terminal.sixteenColor.G = arg - 40;
            }
            case 49 -> { // Default background color
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
                terminal.backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
                terminal.sixteenColor.G = TerminalColors.Color.BLACK;
            }
            case 90, 91, 92, 93, 94, 95, 96, 97 -> { // Set foreground color
                terminal.currentForegroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR_BRIGHT;
                terminal.sixteenColorBright.R = arg - 90;
            }
            case 100, 101, 102, 103, 104, 105, 106, 107 -> { // Set background color
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR_BRIGHT;
                terminal.sixteenColorBright.G = arg - 100;
            }
            default -> {}
        }
    }
}
