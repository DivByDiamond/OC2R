package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;

public class CUB extends CSISequenceHandler {
    public CUB(Terminal terminal) {
        super(terminal);
    }

    public void execute(int[] args, int argsCount, CSIState state) {
        terminal.setClampedCursorPos(terminal.x - Math.max(1, args[0]), terminal.y);
    }
}
