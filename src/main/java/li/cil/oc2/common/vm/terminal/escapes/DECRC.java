package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECRC {
    public static void execute(Terminal terminal) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.x = terminal.altSavedX;
            terminal.y = terminal.altSavedY;
            terminal.style = terminal.altSavedStyle;
            terminal.useG0 = terminal.altSavedUseG0;
            terminal.drawingModeG0 = terminal.altSavedDrawingModeG0;
            terminal.drawingModeG1 = terminal.altSavedDrawingModeG1;
        } else {
            terminal.x = terminal.savedX;
            terminal.y = terminal.savedY;
            terminal.style = terminal.savedStyle;
            terminal.useG0 = terminal.savedUseG0;
            terminal.drawingModeG0 = terminal.savedDrawingModeG0;
            terminal.drawingModeG1 = terminal.savedDrawingModeG1;
        }
    }
}