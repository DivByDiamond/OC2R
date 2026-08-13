package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class ECH extends CSISequenceHandler {
    public ECH(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        // ECH — Erase Character. Fill chars blanks from cursor, no shift.
        // Default arg = 1 per spec.
        int chars = (argCount < 1) ? 1 : Math.max(args[0], 1);
        terminal.bufferManager.clearChars(terminal.y, terminal.x, chars);
    }
}
