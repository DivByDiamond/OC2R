package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH5
        extends CSISequenceHandler { // Combined Handler 5 (XTSMPOINTER, DECSTR, DECSCL, and
    // DECRARA)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH5(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.greaterThan) { // XTSMPOINTER
            LOGGER.warn("XTSMPOINTER not implemented");
        } else if (state.exclamation) { // DECSTR
            LOGGER.warn("DECSTR not implemented");
        } else if (state.quote) { // DECSCL
            LOGGER.warn("DECSCL not implemented");
        } else if (state.dollarSign) { // DECRQM
            int mode = args[0];
            if (state.questionMark) { // DECSET/DECRST
                terminal.io.putResponse(
                        "\033[?"
                                + mode
                                + ";"
                                + terminal.currentPrivateModeState.getModeForRequest(mode)
                                + "$y");
            } else { // SM/RM
                terminal.io.putResponse(
                        "\033["
                                + mode
                                + ";"
                                + terminal.currentModeState.getModeForRequest(mode)
                                + "$y");
            }
        } else { // XTPUSHSGR
            LOGGER.warn("XTPUSHSGR not implemented");
        }
    }
}