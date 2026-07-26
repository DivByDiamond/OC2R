package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;

public class DECRC {
    public static void execute(Terminal terminal) {
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.x = terminal.altSavedX;
            terminal.y = terminal.altSavedY;
        } else {
            terminal.x = terminal.savedX;
            terminal.y = terminal.savedY;
        }
    }
}
