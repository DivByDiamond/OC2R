package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * RCP — Restore Cursor Position ({@code CSI Ps u}), the ANSI alias of DECRC's cursor restore.
 * Restores the cursor column/row saved by SCP ({@code CSI s} / SCOSC, handled in CH6) or DECSC.
 * Per xterm's SCORC this restores the cursor position only (not the rendition that DECRC also
 * restores); the saved coordinates are shared with DECSC/DECRC.
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
        terminal.autowrapPending = false; // restoring the cursor clears any pending wrap
        if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
            terminal.setCursorPos(terminal.altSavedX, terminal.altSavedY);
        } else {
            terminal.setCursorPos(terminal.savedX, terminal.savedY);
        }
    }
}
