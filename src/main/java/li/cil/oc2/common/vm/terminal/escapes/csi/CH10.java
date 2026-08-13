package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CH10 extends CSISequenceHandler { // Combined Handler 10 (DCH and XTPUSHCOLORS)
    public CH10(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.hash) { // XTPUSHCOLORS
            System.out.println("XTPUSHCOLORS not implemented");
        } else { // DCH
            // DCH — Delete Character. Shift remaining chars left, blanks at end.
            // Default arg = 1 per spec.
            int chars = (argsCount < 1) ? 1 : Math.max(args[0], 1);
            terminal.bufferManager.deleteChars(terminal.y, terminal.x, chars);
        }
    }
}
