package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH7
        extends CSISequenceHandler { // Combined Handler 7 (XTVERSION, DECLL, DECSCUSR, DECSCA, and
    // XTPOPSGR)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH7(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.greaterThan) { // XTVERSION
            if (args[0] == 0) {
                terminal.io.putResponse("\033P>|oc2rvt(1.0.0)\033\\");
            }
        } else if (state.space) { // DECSCUSR
            int cursorStyle = args[0];
            if (cursorStyle < 0 || cursorStyle > 6) {
                terminal.cursorMode = TerminalColors.CursorMode.DEFAULT;
                return;
            }
            terminal.cursorMode = cursorStyle;
        } else if (state.quote) { // DECSCA
            LOGGER.warn("DECSCA not implemented");
        } else if (state.hash) { // XTPOPSGR
            LOGGER.warn("XTPOPSGR not implemented");
        } else { // DECLL
            LOGGER.warn("DECLL not implemented");
        }
    }
}