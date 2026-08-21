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

        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        int lines = args[0];
        int maxLines = terminal.scrollLast - terminal.y + 1;
        lines = Math.min(lines, Math.max(0, maxLines));
        if (lines == 0) return;
        if (useAltBuffer) {
            terminal.bufferManager.shiftLines(terminal.y, terminal.scrollLast - lines, lines);
        } else {
            int startRow = terminal.y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
            int endRow = terminal.scrollLast + terminal.lastRowToDisplayMax - Terminal.HEIGHT - lines;
            terminal.bufferManager.shiftLines(startRow, endRow, lines);
        }
    }
}
