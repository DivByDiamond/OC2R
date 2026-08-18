package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH8
        extends CSISequenceHandler { // Combined Handler 8 (SU, XTTITLEPOS, and XTSMGRAPHICS)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH8(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.questionMark) { // XTSMGRAPHICS
            LOGGER.warn("XTSMGRAPHICS not implemented");
        } else if (state.hash) { // XTTITLEPOS
            LOGGER.warn("XTTITLEPOS not implemented");
        } else { // SU
            for (int i = 0; i < Math.max(1, args[0]); i++) {
                if (terminal.lastRowToDisplay
                        < Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT) {
                    terminal.bufferManager.incrementLastLineToDisplay();
                }
                terminal.bufferManager.shiftUpOne();
            }
        }
    }
}
