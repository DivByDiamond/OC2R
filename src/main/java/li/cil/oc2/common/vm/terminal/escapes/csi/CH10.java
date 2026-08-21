package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH10 extends CSISequenceHandler { // Combined Handler 10 (DCH and XTPUSHCOLORS)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH10(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return state.hash ? new int[0] : new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.hash) { // XTPUSHCOLORS
            LOGGER.warn("XTPUSHCOLORS not implemented");
        } else { // DCH — Delete Character: shift remaining chars left, blank the tail
            // deleteChars clamps count to the remaining width, fills with the current
            // background / DEFAULT_FOREGROUND, and marks the rendered screen row dirty.
            terminal.bufferManager.deleteChars(terminal.y, terminal.x, args[0]);
        }
    }
}
