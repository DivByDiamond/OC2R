package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.ModeTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH6
        extends CSISequenceHandler { // Combined Handler 6 (XTSAVE, XTSHIFTESCAPE, DECSLRM, and
    // SCOSC)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH6(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.questionMark) { // XTSAVE
            handleXTSAVE(args[0]);
        } else if (state.greaterThan) { // XTSHIFTESCAPE
            LOGGER.warn("XTSHIFTESCAPE not implemented");
        } else if (argsCount == 2) { // DECSLRM
            LOGGER.warn("DECSLRM not implemented");
        } else if (argsCount == 0) { // SCOSC
            if (!terminal.currentPrivateModeState.DECLRMM) {
                terminal.savedX = terminal.x;
                terminal.savedY = terminal.y;
            }
        }
    }

    private void handleXTSAVE(final int mode) {
        final ModeTable table = ModeTable.forPrivateMode(mode);
        if (table != null) {
            table.set(terminal.savePrivateModeState, table.get(terminal.currentPrivateModeState));
        }
    }
}