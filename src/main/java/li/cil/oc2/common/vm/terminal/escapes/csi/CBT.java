package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * CBT — Cursor Backward Tabulation ({@code CSI Ps Z}). Moves the cursor left to the previous tab
 * stop, {@code Ps} times (default 1), stopping at column 0. The mirror of HT/forward-tab; the tab
 * stops used match those of {@link TerminalOutput}'s forward tab (altTabs in the alt buffer).
 */
class CBT extends CSISequenceHandler {
    CBT(final Terminal terminal) {
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
        int count = args[0] <= 0 ? 1 : args[0];
        for (int i = 0; i < count && terminal.x > 0; i++) {
            int x = terminal.x - 1;
            while (x > 0 && !tabs[x]) {
                x--;
            }
            terminal.x = x;
        }
    }
}
