package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;

public class SGR extends CSISequenceHandler {
    public SGR(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        for (int i = 0; i < Math.max(1, argCount); i++) {
            int v1 = args[i];
            if (v1 == 38 || v1 == 48) {
                int index = i + 1;
                int v2 = args[index];
                if (v1 == 38) {
                    if (v2 == 5) {
                        terminal.currentForegroundColorMode =
                                TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR;
                        terminal.twoFiftySixColor.R = args[++index];
                    } else if (v2 == 2) {
                        terminal.currentForegroundColorMode = TerminalColors.ColorMode.TRUE_COLOR;
                        terminal.foregroundColor =
                                new TerminalColors.ColorData(
                                        args[++index],
                                        args[++index],
                                        args[++index],
                                        TerminalColors.ColorMode.TRUE_COLOR);
                    }
                } else {
                    if (v2 == 5) {
                        terminal.currentBackgroundColorMode =
                                TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR;
                        terminal.twoFiftySixColor.G = args[++index];
                    } else if (v2 == 2) {
                        terminal.currentBackgroundColorMode = TerminalColors.ColorMode.TRUE_COLOR;
                        terminal.backgroundColor =
                                new TerminalColors.ColorData(
                                        args[++index],
                                        args[++index],
                                        args[++index],
                                        TerminalColors.ColorMode.TRUE_COLOR);
                    }
                }
                return;
            }

            selectStyle(terminal, v1);
        }
    }

    private static void selectStyle(Terminal terminal, int arg) {
        switch (arg) {
            case 0 -> { // Reset / Normal
                terminal.sixteenColor = TerminalColors.DEFAULT_COLORS.Copy();
                terminal.sixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.Copy();
                terminal.style = TerminalColors.DEFAULT_STYLE;
                terminal.currentForegroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
                terminal.twoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.Copy();
                terminal.foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.Copy();
                terminal.backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.Copy();
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
                terminal.foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.Copy();
                terminal.sixteenColor.R = TerminalColors.Color.WHITE;
            }
            case 40, 41, 42, 43, 44, 45, 46, 47 -> { // Set background color
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
                terminal.sixteenColor.G = arg - 40;
            }
            case 49 -> { // Default background color
                terminal.currentBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
                terminal.backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.Copy();
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
