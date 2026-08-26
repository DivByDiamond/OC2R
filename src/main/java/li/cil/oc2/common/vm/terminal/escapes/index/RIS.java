package li.cil.oc2.common.vm.terminal.escapes.index;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.escapes.SavedCursor;
import li.cil.oc2.common.vm.terminal.modes.ModeState;
import li.cil.oc2.common.vm.terminal.modes.PrivateModeState;

public class RIS {
    public static void execute(Terminal terminal) {
        terminal.currentForegroundColorMode = TerminalColors.ColorMode.DEFAULT_FOREGROUND;
        terminal.currentBackgroundColorMode = TerminalColors.ColorMode.DEFAULT_BACKGROUND;
        terminal.sixteenColor = TerminalColors.DEFAULT_COLORS.copy();
        terminal.sixteenColorBright = TerminalColors.DEFAULT_BRIGHT_COLORS.copy();
        terminal.backgroundColor = TerminalColors.DEFAULT_TRUE_COLOR_BACKGROUND.copy();
        terminal.foregroundColor = TerminalColors.DEFAULT_TRUE_COLOR_FOREGROUND.copy();
        terminal.twoFiftySixColor = TerminalColors.DEFAULT_256_COLORS.copy();
        terminal.style = TerminalColors.DEFAULT_STYLE;
        terminal.currentModeState = new ModeState();
        terminal.currentPrivateModeState = new PrivateModeState();
        terminal.savePrivateModeState = new PrivateModeState();
        terminal.state = Terminal.State.NORMAL;
        // Return to the 80-column power-on default. setWidth reallocates the buffers, clears
        // the screen, rebuilds tab stops, resets margins, and homes the cursor — the full
        // geometry reset. The DECCOLM flag is cleared above via the fresh PrivateModeState;
        // this keeps the flag and the allocated width in agreement.
        terminal.setWidth(Terminal.WIDTH);
        SavedCursor.reset(terminal);
        terminal.cursorMode = TerminalColors.CursorMode.DEFAULT;
        terminal.input.clear();
        terminal.drawingModeG0 = TerminalColors.DrawingMode.ASCII;
        terminal.drawingModeG1 = TerminalColors.DrawingMode.ASCII;
        terminal.useG0 = true;
    }
}