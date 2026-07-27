package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class VPA extends CSISequenceHandler {
    public VPA(final Terminal terminal) {
        super(terminal);
    }

    public void execute(final int[] args, final int argsCount, final CSIState state) {
        terminal.setClampedCursorPos(terminal.x, args[0] - 1);
    }
}