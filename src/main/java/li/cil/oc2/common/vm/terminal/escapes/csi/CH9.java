package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH9 extends CSISequenceHandler { // Combined Handler 9 (SD, XTHIMOUSE, and XTRMTITLE)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH9(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return state.greaterThan ? new int[0] : new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.greaterThan) { // XTRMTITLE
            LOGGER.warn("XTRMTITLE not implemented");
        } else if (argsCount == 5) { // XTHIMOUSE
            LOGGER.warn("XTHIMOUSE not implemented");
        } else { // SD
            // Clamp: EscapeUtilities.parseArgument saturates at Integer.MAX_VALUE;
            // shifting more than the screen height has no additional effect.
            final int n = Math.min(args[0], Terminal.HEIGHT);
            for (int i = 0; i < n; i++) {
                terminal.bufferManager.shiftDownOne();
            }
        }
    }
}
