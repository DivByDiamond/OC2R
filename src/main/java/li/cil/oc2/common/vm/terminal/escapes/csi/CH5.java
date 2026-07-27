package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CH5
        extends CSISequenceHandler { // Combined Handler 5 (XTSMPOINTER, DECSTR, DECSCL, and
    // DECRARA)
    public CH5(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.greaterThan) { // XTSMPOINTER
            System.out.println("XTSMPOINTER not implemented");
        } else if (state.exclamation) { // DECSTR
            System.out.println("DECSTR not implemented");
        } else if (state.quote) { // DECSCL
            System.out.println("DECSCL not implemented");
        } else if (state.dollarSign) { // DECRQM
            int mode = args[0];
            if (state.questionMark) { // DECSET/DECRST
                terminal.putResponse(
                        "\033[?"
                                + mode
                                + ";"
                                + terminal.currentPrivateModeState.getModeForRequest(mode)
                                + "$y");
            } else { // SM/RM
                terminal.putResponse(
                        "\033["
                                + mode
                                + ";"
                                + terminal.currentModeState.getModeForRequest(mode)
                                + "$y");
            }
        } else { // XTPUSHSGR
            System.out.println("XTPUSHSGR not implemented");
        }
    }
}