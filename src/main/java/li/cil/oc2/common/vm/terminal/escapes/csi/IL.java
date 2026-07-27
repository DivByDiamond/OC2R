package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class IL extends CSISequenceHandler {
    public IL(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        int lines = Math.max(args[0], 1);
        if (useAltBuffer) {
            terminal.bufferManager.shiftLines(terminal.y, terminal.scrollLast - lines, lines);
        } else {
            terminal.bufferManager.shiftLines(
                    terminal.y + terminal.lastRowToDisplayMax - Terminal.HEIGHT,
                    Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT - 2,
                    lines);
        }
    }
}