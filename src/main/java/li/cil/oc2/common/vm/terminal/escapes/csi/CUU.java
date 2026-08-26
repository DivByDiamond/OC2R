package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUU extends CSISequenceHandler {
    public CUU(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(int[] args, int argsCount, CSIState state) {
        // Up by Ps rows. Subtraction can't overflow, but all four cardinal directions share the
        // bounded relative-move primitive (see Terminal.moveCursorBy).
        terminal.moveCursorBy(0, -args[0]);
    }
}
