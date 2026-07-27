package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DL extends CSISequenceHandler {
    public DL(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        terminal.setCursorPos(0, terminal.y);

        int lines = Math.max(args[0], 1);

        for (int i = 0; i < lines; i++) {
            terminal.bufferManager.clearLine(terminal.y + i);
        }

        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        if (useAltBuffer) {
            terminal.bufferManager.shiftLines(terminal.y + lines, terminal.scrollLast, -lines);
        } else {
            terminal.bufferManager.shiftLines(
                    terminal.y + lines, Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT - 1, -lines);
        }
    }
}