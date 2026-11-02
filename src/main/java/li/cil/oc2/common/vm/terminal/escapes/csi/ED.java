package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ED extends CSISequenceHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public ED(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {0};
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        int x = Math.min(terminal.x, terminal.width - 1);
        if (state.questionMark) {
            LOGGER.warn("DECSED is not implemented");
        } else {
            switch (args[0]) {
                case 0 -> { // From cursor to end of screen
                    eraseLine(terminal.y, x, terminal.width);
                    for (int iy = terminal.y + 1; iy < terminal.height; iy++) {
                        eraseLine(iy, 0, terminal.width);
                    }
                }
                case 1 -> { // From beginning of screen to cursor
                    for (int iy = 0; iy < terminal.y; iy++) {
                        eraseLine(iy, 0, terminal.width);
                    }
                    eraseLine(terminal.y, 0, x + 1);
                }
                case 2 -> // Entire screen
                        terminal.bufferManager.clear();
                case 3 -> // Erase scrollback (xterm E3)
                        terminal.bufferManager.clearScrollback();
                default -> {}
            }
        }
    }

    /** ED resets the line rendition only when it erases the entire visible line (VT510 ED). */
    private void eraseLine(final int row, final int from, final int to) {
        final boolean entireLine = from == 0 && to > terminal.lastColumn(row);
        // Resetting to SWL exposes the hidden half, which must receive the erase rendition too.
        terminal.bufferManager.clearLine(row, from, entireLine ? terminal.width : to);
        if (entireLine) {
            final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
            final byte[] attributes = alt ? terminal.altLineAttrs : terminal.lineAttrs;
            final int index = alt ? row : terminal.lastRowToDisplayMax - terminal.height + row;
            if (attributes != null && index >= 0 && index < attributes.length) {
                attributes[index] = Terminal.LINE_ATTR_SINGLE;
            }
        }
    }
}
