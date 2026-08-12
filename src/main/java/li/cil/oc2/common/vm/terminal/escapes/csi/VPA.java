package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class VPA extends CSISequenceHandler {
    public VPA(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        int row = (argsCount > 0 && args[0] > 0) ? args[0] : 1;
        terminal.setClampedCursorPos(terminal.x, row - 1);
    }
}
