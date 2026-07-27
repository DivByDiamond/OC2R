package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class IND {
    public static void execute(Terminal terminal) {
        if (terminal.y >= terminal.scrollLast) {
            terminal.shiftUpOne();
            if (!terminal.currentPrivateModeState.isAltBufferEnabled())
                terminal.incrementLastLineToDisplay();
        } else {
            terminal.setCursorPos(terminal.x, terminal.y + 1);
        }
    }
}