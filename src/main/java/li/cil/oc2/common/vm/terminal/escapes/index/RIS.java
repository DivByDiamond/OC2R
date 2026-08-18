package li.cil.oc2.common.vm.terminal.escapes.index;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.modes.ModeState;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;

public class RIS {
    public static void execute(Terminal terminal) {
        terminal.currentForegroundColorMode = TerminalColors.ColorMode.SIXTEEN_COLOR;
        terminal.currentBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
        terminal.sixteenColor = TerminalColors.DEFAULT_COLORS.copy();
        terminal.sixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
        terminal.backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
        terminal.foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
        terminal.twoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
        terminal.style = TerminalColors.DEFAULT_STYLE;
        terminal.currentModeState = new ModeState();
        terminal.currentPrivateModeState = new PrivateModeState();
        terminal.lastRowToDisplay = 24;
        terminal.lastRowToDisplayMax = 24;
        terminal.scrollFirst = 0;
        terminal.scrollLast = Terminal.HEIGHT - 1;
        terminal.savedX = 0;
        terminal.savedY = 0;
        terminal.altSavedX = 0;
        terminal.altSavedY = 0;
        terminal.cursorMode = TerminalColors.CursorMode.DEFAULT;
        terminal.input.clear();
        terminal.drawingModeG0 = TerminalColors.DrawingMode.ASCII;
        terminal.drawingModeG1 = TerminalColors.DrawingMode.ASCII;
        terminal.useG0 = true;
        terminal.bufferManager.clear();
        terminal.bufferManager.clearAlt();
        terminal.setCursorPos(0, 0);
        Arrays.fill(terminal.buffer, ' ');
        Arrays.fill(terminal.colors, TerminalColors.DEFAULT_COLORS.copy());
        Arrays.fill(terminal.colorsBackground, TerminalColors.DEFAULT_BACKGROUND_COLOR.copy());
        Arrays.fill(terminal.styles, TerminalColors.DEFAULT_STYLE);
        Arrays.fill(terminal.altBuffer, ' ');
        Arrays.fill(terminal.altColors, TerminalColors.DEFAULT_COLORS.copy());
        Arrays.fill(terminal.altColorsBackground, TerminalColors.DEFAULT_BACKGROUND_COLOR.copy());
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