package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;

public class DL extends CSISequenceHandler {
    public DL(final Terminal terminal) {
        super(terminal);
    }

    public void execute(int[] args, int argCount, CSIState state) {
        terminal.setCursorPos(0, terminal.y);

        int lines = Math.max(args[0], 1);

        for (int i = 0; i < lines; i++) {
            terminal.clearLine(terminal.y + i);
        }

        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();

        if (useAltBuffer) {
            terminal.shiftLines(terminal.y + lines, terminal.scrollLast, -lines);
        } else {
            terminal.shiftLines(terminal.y + lines, Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT - 1, -lines);
        }
    }
}
