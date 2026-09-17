package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.escapes.SavedCursor;
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
    public int[] defaultParameters(final CSIState state) {
        if (state.questionMark || state.greaterThan) {
            return new int[0];
        }
        return new int[] {1, terminal.width}; // DECSLRM: Pl defaults 1, Pr defaults width
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.questionMark) { // XTSAVE
            handleXTSAVE(args[0]);
        } else if (state.greaterThan) { // XTSHIFTESCAPE
            LOGGER.warn("XTSHIFTESCAPE not implemented");
        } else if (argsCount == 2) { // DECSLRM
            handleDECSLRM(args);
        } else if (argsCount == 0 && !terminal.currentPrivateModeState.DECLRMM) { // SCOSC
            SavedCursor.save(terminal);
        }
    }

    private void handleXTSAVE(final int mode) {
        final ModeTable table = ModeTable.forPrivateMode(mode);
        if (table != null) {
            table.set(terminal.savePrivateModeState, table.get(terminal.currentPrivateModeState));
        }
    }

    // DEC VT420-RM: DECSLRM is ignored while DECLRMM (mode 69) is reset — with left/right margin
    // mode off, `CSI Pl;Pr s` with 2 numeric args still parses (argsCount == 2 above) but has no
    // effect, matching xterm's CASE_DECSLRM guard on curXtermPrivate.rectangle_mode-adjacent
    // decLeftRightMargin.
    private void handleDECSLRM(final int... args) {
        if (!terminal.currentPrivateModeState.DECLRMM) {
            return;
        }
        final int left = args[0] - 1;
        final int right = Math.min(args[1], terminal.width) - 1;
        if (right - left <= 0) {
            return; // Pl >= Pr is an error per VT420-RM — leave margins unchanged
        }
        terminal.scrollColFirst = left;
        terminal.scrollColLast = right;
        terminal.setRelativeCursorPos(0, 0); // home cursor, like DECSTBM
    }
}