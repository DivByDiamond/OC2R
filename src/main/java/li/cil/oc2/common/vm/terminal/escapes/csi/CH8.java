package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CH8
        extends CSISequenceHandler { // Combined Handler 8 (SU, XTTITLEPOS, and XTSMGRAPHICS)
    public CH8(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.questionMark) { // XTSMGRAPHICS
            System.out.println("XTSMGRAPHICS not implemented");
        } else if (state.hash) { // XTTITLEPOS
            System.out.println("XTTITLEPOS not implemented");
        } else { // SU
            for (int i = 0; i < Math.max(1, args[0]); i++) {
                terminal.bufferManager.shiftUpOne();
            }
        }
    }
}
