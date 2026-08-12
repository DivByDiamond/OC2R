package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUP extends CSISequenceHandler {
    public CUP(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argsCount, CSIState state) {
        int row = (argsCount > 0 && args[0] > 0) ? args[0] : 1;
        int col = (argsCount > 1 && args[1] > 0) ? args[1] : 1;
        terminal.setRelativeCursorPos(col - 1, row - 1);
    }
}
