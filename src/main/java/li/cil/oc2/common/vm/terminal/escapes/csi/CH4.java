package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.Locale;
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
    public void execute(final int[] args, final int argsCount, final CSIState state) { // NOPMD: CyclomaticComplexity, CognitiveComplexity — multi-sub-handler dispatch (XTSMTITLE/DECSWBV/DECRARA/XTWINOPS) with a switch; inherent to the combined-handler architecture
        // xterm-410 VTPrsTbl.c maps 't' to a handler in exactly four tables: plain CSI
        // (XTERM_WINOPS), SP (DECSWBV), $ (DECRARA), and CSI > (XTERM_SM_TITLE). Every other
        // intermediate table — ?, !, #, ", ', * — maps 't' to CASE_GROUND_STATE (ignored);
        // the mixed CSI ? $ table does too. Those forms must NOT reach the branches below:
        // pre-case-8 a misrouted reply was harmless, but a misrouted RESIZE would be exactly
        // the "more permissive than xterm" trap CH13's guard documents.
        if (state.questionMark || state.hash || state.quote || state.singleQuote
                || state.exclamation || state.asterisk) {
            return;
        }
        if (state.greaterThan) { // XTSMTITLE
            LOGGER.warn("XTSMTITLE is not implemented");
        } else if (state.space) { // DECSWBV
            LOGGER.warn("DECSWBV is not implemented yet");
        } else if (state.dollarSign) { // DECRARA
            LOGGER.warn("DECRARA is not implemented");
        } else { // XTWINOPS
            switch (args[0]) {
                case 8 -> { // ewSetWinSizeChars: resize text area in characters
                    // xterm-410 charproc.c:9044: RequestResize(xw, optional_param(1),
                    // optional_param(2), True). A missing/zero param leaves that dimension
                    // unchanged (optional_param returns DEFAULT, RequestResize skips
                    // non-positive values). Rows first, then cols — matching xterm's
                    // parameter order. Both dims are range-checked HERE (xterm give_up on
                    // oversized requests): the escape channel carries arbitrary ints, and an
                    // unchecked col count would reach resizeWidth's allocation straight from
                    // the guest. Does NOT set DECCOLM (only DECSCPP and DECCOLM mode 3 do).
                    final int rows = argsCount > 1 ? args[1] : 0;
                    final int cols = argsCount > 2 ? args[2] : 0;
                    if (rows >= 1 && rows <= Terminal.MAX_HEIGHT) {
                        terminal.resizeHeight(rows);
                    }
                    if (cols >= 1 && cols <= Terminal.MAX_WIDTH) {
                        terminal.resizeWidth(cols);
                    }
                }
                case 14 ->
                        terminal.io.putResponse(
                                String.format(Locale.ROOT, "\033[4;%d;%dt",
                                        terminal.height * Terminal.CHAR_HEIGHT,
                                        terminal.width * Terminal.CHAR_WIDTH));
                case 15 ->
                        terminal.io.putResponse(
                                String.format(Locale.ROOT, "\033[5;%d;%dt",
                                        terminal.height * Terminal.CHAR_HEIGHT,
                                        terminal.width * Terminal.CHAR_WIDTH));
                case 16 ->
                        terminal.io.putResponse(
                                String.format(Locale.ROOT, "\033[6;%d;%dt",
                                        Terminal.CHAR_HEIGHT, Terminal.CHAR_WIDTH));
                case 18 ->
                        terminal.io.putResponse(
                                String.format(Locale.ROOT, "\033[8;%d;%dt",
                                        terminal.height, terminal.width));
                case 19 ->
                        terminal.io.putResponse(
                                String.format(Locale.ROOT, "\033[9;%d;%dt",
                                        terminal.height, terminal.width));
                default -> {}
            }
        }
    }
}
