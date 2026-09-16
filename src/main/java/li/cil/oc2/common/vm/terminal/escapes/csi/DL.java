package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DL extends CSISequenceHandler {
    public DL(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (terminal.y < terminal.scrollFirst || terminal.y > terminal.scrollLast) return;

        int lines = args[0];
        int maxLines = terminal.scrollLast - terminal.y + 1;
        lines = Math.min(lines, Math.max(0, maxLines));
        if (lines == 0) return;

        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        // No pre-clear of the deleted rows: the shift's copy overwrites them (or the shift's
        // blank fill does when the region tail is short), and shiftLines marks exactly the
        // touched rows dirty. The cleared-then-overwritten loop was dead work.
        if (useAltBuffer) {
            terminal.bufferManager.shiftLines(terminal.y + lines, terminal.scrollLast, -lines,
                    terminal.y, terminal.scrollLast);
        } else {
            int off = terminal.lastRowToDisplayMax - terminal.height;
            terminal.bufferManager.shiftLines(terminal.y + lines + off, terminal.scrollLast + off,
                    -lines, terminal.y + off, terminal.scrollLast + off);
        }
    }
}
