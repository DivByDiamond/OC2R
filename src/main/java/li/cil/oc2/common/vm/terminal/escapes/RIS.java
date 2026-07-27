package li.cil.oc2.common.vm.terminal.escapes;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;
import li.cil.oc2.common.vm.terminal.modes.ModeState;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;

public class RIS {
    public static void execute(Terminal terminal) {
        terminal.currentForegroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
        terminal.currentBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
        terminal.Use1006 = false;
        terminal.sixteenColor = TerminalColors.DEFAULT_COLORS.Copy();
        terminal.sixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.Copy();
        terminal.backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.Copy();
        terminal.foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.Copy();
        terminal.twoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.Copy();
        terminal.style = TerminalColors.DEFAULT_STYLE;
        terminal.currentModeState = new ModeState();
        terminal.currentPrivateModeState = new PrivateModeState();
        terminal.lastRowToDisplay = 24;
        terminal.lastRowToDisplayMax = 24;
        terminal.drawingModeG0 = TerminalColors.DrawingMode.ASCII;
        terminal.drawingModeG1 = TerminalColors.DrawingMode.ASCII;
        terminal.useG0 = true;
        terminal.clear();
        terminal.clearAlt();
        Arrays.fill(terminal.buffer, ' ');
        Arrays.fill(terminal.colors, TerminalColors.DEFAULT_COLORS.Copy());
        Arrays.fill(terminal.colorsBackground, TerminalColors.DEFAULT_BACKGROUND_COLOR.Copy());
        Arrays.fill(terminal.styles, TerminalColors.DEFAULT_STYLE);
        Arrays.fill(terminal.altBuffer, ' ');
        Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_COLORS.Copy());
        Arrays.fill(terminal.altColorsBackground, TerminalColors.DEFAULT_BACKGROUND_COLOR.Copy());
        Arrays.fill(terminal.altStyles, TerminalColors.DEFAULT_STYLE);
        Arrays.fill(terminal.tabs, false);
        Arrays.fill(terminal.altTabs, false);
        for (int i = 1; i < Terminal.WIDTH; i++) {
            if (i % TerminalColors.TAB_WIDTH == 0) {
                terminal.tabs[i] = true;
                terminal.altTabs[i] = true;
            }
        }
    }
}