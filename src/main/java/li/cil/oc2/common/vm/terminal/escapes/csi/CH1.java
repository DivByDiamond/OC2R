package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.ModeTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH1 extends CSISequenceHandler { // Combined Handler 1 (DECSTBM & XTRESTORE)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH1(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        if (state.questionMark || state.dollarSign) {
            return new int[0];
        }
        return new int[] {1, Terminal.HEIGHT};
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (state.questionMark) { // XTRESTORE
            handleXTRESTORE(args[0]);
        } else if (state.dollarSign) { // DECCARA
            LOGGER.warn("DECCARA is not implemented");
        } else { /* DECSTBM with 0 or 1 args = reset to full screen */
            handleDECSTBM(args);
        }
    }

    private void handleXTRESTORE(int mode) {
        final ModeTable table = ModeTable.forPrivateMode(mode);
        if (table != null) {
            table.set(terminal.currentPrivateModeState, table.get(terminal.savePrivateModeState));
        }
    }

    private void handleDECSTBM(int... args) {
        final int first;
        final int last;
        /* Each parameter defaults independently: top=1, bottom=HEIGHT */
        final int top = args[0];
        final int bottom = Math.min(args[1], Terminal.HEIGHT);
        first = top - 1;
        last = bottom - 1;
        if (last - first <= 0) {
            return;
        }
        terminal.scrollFirst = first; // to index
        terminal.scrollLast = last; // to index
        terminal.setRelativeCursorPos(0, 0); // send cursor home
    }
}
