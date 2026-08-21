package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH4
        extends CSISequenceHandler { // Combined Handler 4 (XTWINOPS, XTSMTITLE, DECSWBV, and
    // DECRARA)
    private static final Logger LOGGER = LogManager.getLogger();

    public CH4(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.greaterThan) { // XTSMTITLE
            LOGGER.warn("XTSMTITLE is not implemented");
        } else if (state.space) { // DECSWBV
            LOGGER.warn("DECSWBV is not implemented yet");
        } else if (state.dollarSign) { // DECRARA
            LOGGER.warn("DECRARA is not implemented");
        } else { // XTWINOPS
            switch (args[0]) {
                case 14 ->
                        terminal.io.putResponse(
                                "\033[4;"
                                        + (Terminal.HEIGHT * Terminal.CHAR_HEIGHT)
                                        + ";"
                                        + (terminal.width * Terminal.CHAR_WIDTH)
                                        + "t");
                case 15 ->
                        terminal.io.putResponse(
                                "\033[5;"
                                        + (Terminal.HEIGHT * Terminal.CHAR_HEIGHT)
                                        + ";"
                                        + (terminal.width * Terminal.CHAR_WIDTH)
                                        + "t");
                case 16 ->
                        terminal.io.putResponse(
                                "\033[6;" + Terminal.CHAR_HEIGHT + ";" + Terminal.CHAR_WIDTH + "t");
                case 18 ->
                        terminal.io.putResponse(
                                "\033[8;" + Terminal.HEIGHT + ";" + terminal.width + "t");
                case 19 ->
                        terminal.io.putResponse(
                                "\033[9;" + Terminal.HEIGHT + ";" + terminal.width + "t");
                default -> {}
            }
        }
    }
}
