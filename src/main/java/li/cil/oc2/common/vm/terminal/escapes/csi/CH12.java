package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.escapes.SavedCursor;

/**
 * Combined Handler 12 (SCORC and XTRESTORE) — the {@code u} final, grouped by modifier like the
 * other {@code CH*} handlers (cf. {@link CH6}, which groups {@code s} = SCOSC + {@code ?s} XTSAVE):
 * <ul>
 *   <li>{@code CSI ? Ps u} — XTRESTORE: restore private mode {@code Ps} from the saved state into
 *       the current state (the mirror of CH6's {@code ?s} XTSAVE).</li>
 *   <li>{@code CSI Ps u} — SCORC: restore cursor (RCP), the ANSI alias of DECRC. Routes through
 *       {@link SavedCursor#restore} so SCORC and DECRC cannot diverge.</li>
 * </ul>
 * Combining the two meanings of {@code u} in one handler (rather than an RCP class with a
 * {@code questionMark} guard) makes the {@code ?u} branch a real handler instead of a defensive
 * no-op, and mirrors the {@code s}/{@code ?s} structure of CH6.
 */
public class CH12 extends CSISequenceHandler {
    public CH12(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        // XTRESTORE takes a mode number; SCORC (plain u) takes no parameter. An empty array
        // means CSIManager won't default-normalize: XTRESTORE reads args[0] as-is (0 = no mode,
        // a valid "restore nothing"), and SCORC ignores args entirely.
        return new int[0];
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.questionMark) { // XTRESTORE — restore private mode(s) from the saved state
            XTRESTORE.execute(terminal, args[0]);
        } else { // SCORC — restore cursor (RCP, the ANSI alias of DECRC)
            SavedCursor.restore(terminal);
        }
    }
}
