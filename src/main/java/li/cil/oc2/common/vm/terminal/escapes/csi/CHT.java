package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * CHT — Cursor Forward Tabulation ({@code CSI Ps I}). Moves the cursor right to the next tab stop,
 * {@code Ps} times (default 1), stopping at the last column. The mirror of CBT (backward) and the
 * CSI form of HT; tab stops match {@link TerminalOutput}'s forward tab (altTabs in the alt buffer).
 */
public class CHT extends CSISequenceHandler {
    public CHT(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        terminal.autowrapPending = false; // cursor move — clears the pending wrap (xterm ResetWrap)
        final boolean[] tabs = terminal.currentPrivateModeState.isAltBufferEnabled()
                ? terminal.altTabs : terminal.tabs;
        final int last = terminal.width - 1;
        for (int i = 0; i < args[0] && terminal.x < last; i++) {
            int x = terminal.x + 1;
            while (x < last && !tabs[x]) {
                x++;
            }
            terminal.x = x;
        }
    }
}
