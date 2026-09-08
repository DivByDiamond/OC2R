package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CH13
        extends CSISequenceHandler { // Combined Handler 13 (DECSCPP and DECSLPP/DECSNLS) — the '|'
    // final is shared by the column-count selector ($ |) and the line-count selector (* |),
    // branched on the intermediate byte.
    private static final Logger LOGGER = LogManager.getLogger();

    public CH13(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(final CSIState state) {
        // DECSCPP: Ps omitted (or 0) defaults to 80 columns. CSIManager substitutes this before
        // execute, so a bare "CSI $ |" selects 80 columns (per DEC VT510-RM / xterm-410
        // CASE_DECSCPP: default, 0, and 80 all mean 80; 132 means 132; anything else is ignored).
        return new int[] {Terminal.WIDTH};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        // xterm-410 routes the private-marker forms elsewhere: csi_dec_dollar_table (the
        // "CSI ? Pn $" table, VTPrsTbl.c:4374) maps '|' to CASE_GROUND_STATE — ignored — and
        // only the plain csi_dollar_table (VTPrsTbl.c:3074) maps '|' to CASE_DECSCPP; the
        // "> Pn $" form has no DECSCPP mapping either. Ignoring the prefixed forms (silently,
        // as xterm does) keeps us from being more permissive than the reference: a mangled or
        // probing sequence must not resize the terminal.
        if (state.questionMark || state.greaterThan) {
            return;
        }
        // '$' (0x24) and '*' (0x2A) are mutually exclusive intermediates — a sequence with
        // both (e.g. CSI *$|) is malformed and must be ignored as xterm does.
        if (state.dollarSign && state.asterisk) {
            return;
        }
        if (state.dollarSign) { // DECSCPP — Select Columns Per Page (CSI Pn $ |)
            final int cols = args[0];
            if (cols == Terminal.WIDTH) {
                terminal.resizeWidth(Terminal.WIDTH);
                terminal.currentPrivateModeState.DECCOLM = false;
            } else if (cols == 132) {
                terminal.resizeWidth(132);
                terminal.currentPrivateModeState.DECCOLM = true;
            }
            // Any other value is ignored (xterm sets value = -1 and skips the resize).
        } else if (state.asterisk) { // DECSLPP / DECSNLS — Set Lines Per Page (CSI Pn * |)
            // Deferred: dynamic HEIGHT is a separate, larger axis (HEIGHT is static final and
            // pervades buffer sizing, scrollback, the dirty BitSet, renderer rows, the network
            // diff). 132x48 etc. is the eventual payoff. The '*' intermediate is parsed so this
            // stub is reachable; implementation is a future session.
            LOGGER.warn("DECSLPP not implemented");
        }
        // A bare "CSI |" (no intermediate) is not a defined sequence; ignore it.
    }
}
