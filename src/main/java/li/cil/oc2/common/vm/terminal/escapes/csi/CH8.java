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
    public int[] defaultParameters(CSIState state) {
        if (state.questionMark || state.hash) {
            return new int[0];
        }
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.questionMark) { // XTSMGRAPHICS
            LOGGER.warn("XTSMGRAPHICS not implemented");
        } else if (state.hash) { // XTTITLEPOS
            LOGGER.warn("XTTITLEPOS not implemented");
        } else { // SU
            // Clamp: EscapeUtilities.parseArgument saturates at Integer.MAX_VALUE and the
            // dispatcher substitutes 1 for missing/zero args, so the domain is [1, height] —
            // a huge SU scrolls at most one screen and preserves scrollback beyond that.
            // Scrollback window growth and region clamping live in the buffer layer (shiftUp).
            terminal.bufferManager.shiftUp(Math.clamp(args[0], 1, terminal.height));
        }
    }
}
