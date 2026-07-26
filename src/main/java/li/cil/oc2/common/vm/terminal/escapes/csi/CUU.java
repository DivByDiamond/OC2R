package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;

public class CUU extends CSISequenceHandler {
    public CUU(final Terminal terminal) {
        super(terminal);
    }

    public void execute(int[] args, int argsCount, CSIState state) {
        terminal.setClampedCursorPos(terminal.x, terminal.y - Math.max(1, args[0]));
    }
}
