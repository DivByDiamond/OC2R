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

        for (int i = 0; i < lines; i++) {
            terminal.bufferManager.clearLine(terminal.y + i);
        }

        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        if (useAltBuffer) {
            terminal.bufferManager.shiftLines(terminal.y + lines, terminal.scrollLast, -lines);
        } else {
            int startRow = terminal.y + lines + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
            int endRow = terminal.scrollLast + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
            terminal.bufferManager.shiftLines(startRow, endRow, -lines);
        }
    }
}
