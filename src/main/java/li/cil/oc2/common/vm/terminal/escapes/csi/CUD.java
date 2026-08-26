package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUD extends CSISequenceHandler {
    public CUD(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(int[] args, int argsCount, CSIState state) {
        // Down by Ps rows. Bounded relative move (see Terminal.moveCursorBy): a saturated CSI
        // count can't overflow the int sum before setClampedCursorPos clamps.
        terminal.moveCursorBy(0, args[0]);
    }
}
