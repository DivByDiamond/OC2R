package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class RI {
    public static void execute(Terminal terminal) {
        if (terminal.y <= terminal.scrollFirst) {
            terminal.shiftDownOne();
        } else {
            terminal.setCursorPos(0, terminal.y - 1);
        }
    }
}
