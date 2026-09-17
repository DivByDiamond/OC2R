package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class VPA extends CSISequenceHandler {
    public VPA(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        // xRelative=false: VPA only repositions the row, x stays absolute — DECOM's left-margin
        // origin (see Terminal#setRelativeCursorPos) does not apply to a column it didn't set.
        terminal.setRelativeCursorPos(terminal.x, args[0] - 1, false);
    }
}
