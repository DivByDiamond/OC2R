package li.cil.oc2.common.vm.terminal.escapes.index;

import li.cil.oc2.common.vm.terminal.Terminal;

public class IND {
    public static void execute(Terminal terminal) {
        if (terminal.y == terminal.scrollLast) {
            terminal.bufferManager.shiftUpOne();
            if (!terminal.currentPrivateModeState.isAltBufferEnabled())
                terminal.bufferManager.incrementLastLineToDisplay();
            terminal.x = Math.min(terminal.x, Terminal.WIDTH - 1);
        } else {
            terminal.setCursorPos(terminal.x, Math.min(terminal.y + 1, Terminal.HEIGHT - 1));
        }
    }
}