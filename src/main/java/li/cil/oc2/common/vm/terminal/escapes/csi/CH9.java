package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CH9 extends CSISequenceHandler { // Combined Handler 9 (SD, XTHIMOUSE, and XTRMTITLE)
    public CH9(final Terminal terminal) {
        super(terminal);
    }

    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.greaterThan) { // XTRMTITLE
            System.out.println("XTRMTITLE not implemented");
        } else if (argsCount == 5) { // XTHIMOUSE
            System.out.println("XTHIMOUSE not implemented");
        } else { // SD
            for (int i = 0; i < args[0]; i++) {
                terminal.shiftDownOne();
            }
        }
    }
}
