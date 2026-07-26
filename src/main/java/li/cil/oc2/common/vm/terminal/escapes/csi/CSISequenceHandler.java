package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public abstract class CSISequenceHandler {
    protected Terminal terminal;

    public CSISequenceHandler(Terminal terminal) {
        this.terminal = terminal;
    }

    public abstract void execute(int[] args, int argsCount, CSIState state);
}
