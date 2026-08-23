package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECRC {
    public static void execute(Terminal terminal) {
        terminal.autowrapPending = false; // restoring the cursor clears any pending wrap
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.x = terminal.altSavedX;
            terminal.y = terminal.altSavedY;
            terminal.style = terminal.altSavedStyle;
            terminal.useG0 = terminal.altSavedUseG0;
            terminal.drawingModeG0 = terminal.altSavedDrawingModeG0;
            terminal.drawingModeG1 = terminal.altSavedDrawingModeG1;
            terminal.currentForegroundColorMode = terminal.altSavedForegroundColorMode;
            terminal.currentBackgroundColorMode = terminal.altSavedBackgroundColorMode;
            terminal.sixteenColor = terminal.altSavedSixteenColor.copy();
            terminal.sixteenColorBright = terminal.altSavedSixteenColorBright.copy();
            terminal.twoFiftySixColor = terminal.altSavedTwoFiftySixColor.copy();
            terminal.foregroundColor = terminal.altSavedForegroundColor.copy();
            terminal.backgroundColor = terminal.altSavedBackgroundColor.copy();
        } else {
            terminal.x = terminal.savedX;
            terminal.y = terminal.savedY;
            terminal.style = terminal.savedStyle;
            terminal.useG0 = terminal.savedUseG0;
            terminal.drawingModeG0 = terminal.savedDrawingModeG0;
            terminal.drawingModeG1 = terminal.savedDrawingModeG1;
            terminal.currentForegroundColorMode = terminal.savedForegroundColorMode;
            terminal.currentBackgroundColorMode = terminal.savedBackgroundColorMode;
            terminal.sixteenColor = terminal.savedSixteenColor.copy();
            terminal.sixteenColorBright = terminal.savedSixteenColorBright.copy();
            terminal.twoFiftySixColor = terminal.savedTwoFiftySixColor.copy();
            terminal.foregroundColor = terminal.savedForegroundColor.copy();
            terminal.backgroundColor = terminal.savedBackgroundColor.copy();
        }
    }
}
