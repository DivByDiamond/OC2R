package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUF extends CSISequenceHandler {
    public CUF(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(int[] args, int argsCount, CSIState state) {
        // Right by Ps columns. Bounded relative move (see Terminal.moveCursorBy): a saturated
        // CSI count can't overflow the int sum before setClampedCursorPos clamps.
        terminal.moveCursorBy(args[0], 0);
    }
}
