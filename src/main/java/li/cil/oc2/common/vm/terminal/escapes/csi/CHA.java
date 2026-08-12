package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CHA extends CSISequenceHandler {
    public CHA(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        int col = (argsCount > 0 && args[0] > 0) ? args[0] : 1;
        terminal.setClampedCursorPos(col - 1, terminal.y);
    }
}
