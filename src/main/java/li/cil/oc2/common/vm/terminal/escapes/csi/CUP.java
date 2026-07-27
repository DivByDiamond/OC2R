package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUP extends CSISequenceHandler {
    public CUP(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argsCount, CSIState state) {
        terminal.setRelativeCursorPos(args[1] - 1, args[0] - 1);
    }
}