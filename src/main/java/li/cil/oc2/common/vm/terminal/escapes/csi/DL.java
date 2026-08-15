package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DL extends CSISequenceHandler {
    public DL(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        terminal.setCursorPos(0, terminal.y);

        int lines = (argCount < 1) ? 1 : Math.max(args[0], 1);
        int maxLines = terminal.scrollLast - terminal.y + 1;
        lines = Math.clamp(lines, 0, maxLines);
        if (lines == 0) return;

        for (int i = 0; i < lines; i++) {
            terminal.bufferManager.clearLine(terminal.y + i);
        }

        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        if (useAltBuffer) {
            terminal.bufferManager.shiftLines(terminal.y + lines, terminal.scrollLast, -lines);
        } else {
            int startRow = (terminal.y + lines) + (terminal.lastRowToDisplayMax - Terminal.HEIGHT);
            int endRow = terminal.scrollLast + (terminal.lastRowToDisplayMax - Terminal.HEIGHT);
            terminal.bufferManager.shiftLines(startRow, endRow, -lines);
        }
    }
}
