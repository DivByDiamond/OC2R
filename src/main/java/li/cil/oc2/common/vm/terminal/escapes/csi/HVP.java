package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;

public class HVP extends CSISequenceHandler {
    public HVP(final Terminal terminal) {
        super(terminal);
    }

    public void execute(int[] args, int argsCount, CSIState state) {
        terminal.setRelativeCursorPos(args[1] - 1, args[0] - 1);
    }
}
