package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.modes.ModeState;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;
import li.cil.oc2.common.vm.terminal.Terminal;

import java.util.Arrays;

public class RIS {
    public static void execute(Terminal terminal) {
        terminal.currentForegroundColorMode = Terminal.ColorMode.SIXTEEN_COLOR;
        terminal.currentBackgroundColorMode = Terminal.ColorMode.DEFAULT_BACKGROUND;
        terminal.Use1006 = false;
        terminal.sixteenColor = Terminal.DEFAULT_COLORS.Copy();
        terminal.sixteenColorBright = Terminal.DEFAULT_BRIGHT_COLORS.Copy();
        terminal.backgroundColor = Terminal.DEFAULT_TRUE_COLOR_BACKGROUND.Copy();
        terminal.foregroundColor = Terminal.DEFAULT_TRUE_COLOR_FOREGROUND.Copy();
        terminal.twoFiftySixColor = Terminal.DEFAULT_256_COLORS.Copy();
        terminal.style = Terminal.DEFAULT_STYLE;
        terminal.currentModeState = new ModeState();
        terminal.currentPrivateModeState = new PrivateModeState();
        terminal.lastRowToDisplay = 24;
        terminal.lastRowToDisplayMax = 24;
        terminal.drawingModeG0 = Terminal.DrawingMode.ASCII;
        terminal.drawingModeG1 = Terminal.DrawingMode.ASCII;
        terminal.useG0 = true;
        terminal.clear();
        terminal.clearAlt();
        Arrays.fill(terminal.buffer, ' ');
        Arrays.fill(terminal.colors, Terminal.DEFAULT_COLORS.Copy());
        Arrays.fill(terminal.colorsBackground, Terminal.DEFAULT_BACKGROUND_COLOR.Copy());
        Arrays.fill(terminal.styles, Terminal.DEFAULT_STYLE);
        Arrays.fill(terminal.altBuffer, ' ');
        Arrays.fill(terminal.altColors, Terminal.DEFAULT_COLORS.Copy());
        Arrays.fill(terminal.altColorsBackground, Terminal.DEFAULT_BACKGROUND_COLOR.Copy());
        Arrays.fill(terminal.altStyles, Terminal.DEFAULT_STYLE);
        Arrays.fill(terminal.tabs, false);
        Arrays.fill(terminal.altTabs, false);
        for (int i = 1; i < Terminal.WIDTH; i++) {
            if (i % Terminal.TAB_WIDTH == 0) {
                terminal.tabs[i] = true;
                terminal.altTabs[i] = true;
            }
        }
    }
}
