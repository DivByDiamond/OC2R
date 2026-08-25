package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.escapes.SavedCursor;

/**
 * RCP — Restore Cursor Position ({@code CSI Ps u}), the ANSI alias of DECRC. Restores the full
 * saved cursor state (position + rendition) saved by SCP ({@code CSI s} / SCOSC) or DECSC — the
 * same scope as DECRC, matching xterm-410 where SCORC and DECRC both use the {@code DECSC_FLAGS}
 * restore (not position-only). Routes through {@link SavedCursor#restore} so SCORC and DECRC
 * cannot diverge.
 */
public class RCP extends CSISequenceHandler {
    public RCP(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(final CSIState state) {
        return new int[0];
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        SavedCursor.restore(terminal);
    }
}
