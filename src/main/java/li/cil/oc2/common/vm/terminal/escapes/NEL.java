package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class NEL {
    public static void execute(Terminal terminal) {
        if (terminal.y >= terminal.scrollLast) {
            terminal.bufferManager.shiftUpOne();
            if (!terminal.currentPrivateModeState.isAltBufferEnabled())
                terminal.bufferManager.incrementLastLineToDisplay();
            terminal.setCursorPos(0, terminal.y);
        } else {
            terminal.setCursorPos(0, terminal.y + 1);
        }
    }
}