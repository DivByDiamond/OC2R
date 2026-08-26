package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CUB extends CSISequenceHandler {
    public CUB(Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(int[] args, int argsCount, CSIState state) {
        // Left by Ps columns. Subtraction can't overflow, but all four cardinal directions share
        // the bounded relative-move primitive (see Terminal.moveCursorBy).
        terminal.moveCursorBy(-args[0], 0);
    }
}
