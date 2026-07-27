package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CHA extends CSISequenceHandler {
    public CHA(final Terminal terminal) {
        super(terminal);
    }

    public void execute(final int[] args, final int argsCount, final CSIState state) {
        terminal.setClampedCursorPos(args[0] - 1, terminal.y);
    }
}