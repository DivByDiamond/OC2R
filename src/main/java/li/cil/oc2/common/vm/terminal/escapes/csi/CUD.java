package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUD extends CSISequenceHandler {
    public CUD(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argsCount, CSIState state) {
        terminal.setClampedCursorPos(terminal.x, terminal.y + Math.max(1, args[0]));
    }
}