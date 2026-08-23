package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * CHT — Cursor Forward Tabulation ({@code CSI Ps I}). Moves the cursor right to the next tab stop,
 * {@code Ps} times (default 1), stopping at the last column. The mirror of CBT (backward) and the
 * CSI form of HT; tab stops match {@link TerminalOutput}'s forward tab (altTabs in the alt buffer).
 */
class CHT extends CSISequenceHandler {
    CHT(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(final CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        terminal.autowrapPending = false; // cursor move — clears the pending wrap (xterm ResetWrap)
        final boolean[] tabs = terminal.currentPrivateModeState.isAltBufferEnabled()
                ? terminal.altTabs : terminal.tabs;
        final int last = terminal.width - 1;
        int count = args[0] <= 0 ? 1 : args[0];
        for (int i = 0; i < count && terminal.x < last; i++) {
            int x = terminal.x + 1;
            while (x < last && !tabs[x]) {
                x++;
            }
            terminal.x = x;
        }
    }
}
