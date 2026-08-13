package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECSC {
    public static void execute(Terminal terminal) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.altSavedX = terminal.x;
            terminal.altSavedY = terminal.y;
            terminal.altSavedStyle = terminal.style;
            terminal.altSavedUseG0 = terminal.useG0;
            terminal.altSavedDrawingModeG0 = terminal.drawingModeG0;
            terminal.altSavedDrawingModeG1 = terminal.drawingModeG1;
        } else {
            terminal.savedX = terminal.x;
            terminal.savedY = terminal.y;
            terminal.savedStyle = terminal.style;
            terminal.savedUseG0 = terminal.useG0;
            terminal.savedDrawingModeG0 = terminal.drawingModeG0;
            terminal.savedDrawingModeG1 = terminal.drawingModeG1;
        }
    }
}