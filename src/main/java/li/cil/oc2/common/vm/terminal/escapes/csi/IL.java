package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class IL extends CSISequenceHandler {
    public IL(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (terminal.y < terminal.scrollFirst || terminal.y > terminal.scrollLast) return;
        // DECSLRM (§44): IL only respects horizontal margins when the cursor is inside them —
        // xterm-410 xtermInsDelChars gates on curX being within the left/right margins, same
        // shape as the vertical scrollFirst/scrollLast check above.
        if (terminal.x < terminal.scrollColFirst || terminal.x > terminal.scrollColLast) {
            shiftFullRows(args[0]);
            return;
        }

        int lines = args[0];
        int maxLines = terminal.scrollLast - terminal.y + 1;
        lines = Math.min(lines, Math.max(0, maxLines));
        if (lines == 0) return;
        if (terminal.scrollColFirst == 0 && terminal.scrollColLast == terminal.width - 1) {
            shiftFullRows(lines);
        } else {
            shiftWithinColumnMargin(lines);
        }
    }

    // Column-bounded: move only [scrollColFirst, scrollColLast] of each row, one row at a time
    // (see TerminalBuffer#copyRowRange) — a full-row shiftLines would also move the untouched
    // margin columns and drag scrollback rows through the region.
    private void shiftWithinColumnMargin(final int lines) {
        final int width = terminal.scrollColLast - terminal.scrollColFirst + 1;
        for (int i = terminal.scrollLast; i >= terminal.y + lines; i--) {
            terminal.bufferManager.copyRowRange(i - lines, i, terminal.scrollColFirst, width);
        }
        for (int i = terminal.y; i < terminal.y + lines; i++) {
            terminal.bufferManager.clearChars(i, terminal.scrollColFirst, width);
        }
    }

    private void shiftFullRows(final int requestedLines) {
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        int lines = requestedLines;
        int maxLines = terminal.scrollLast - terminal.y + 1;
        lines = Math.min(lines, Math.max(0, maxLines));
        if (lines == 0) return;
        if (useAltBuffer) {
            terminal.bufferManager.shiftLines(terminal.y, terminal.scrollLast - lines, lines,
                    terminal.y, terminal.scrollLast);
        } else {
            int off = terminal.lastRowToDisplayMax - terminal.height;
            terminal.bufferManager.shiftLines(terminal.y + off, terminal.scrollLast + off - lines,
                    lines, terminal.y + off, terminal.scrollLast + off);
        }
    }
}
