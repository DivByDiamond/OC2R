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
    public void execute(int[] args, int argCount, CSIState state) {
        int x = Math.min(terminal.x, Terminal.WIDTH - 1);
        if (state.questionMark) {
            LOGGER.warn("DECSED is not implemented");
        } else {
            switch (args[0]) {
                case 0 -> { // From cursor to end of screen
                    terminal.bufferManager.clearLine(terminal.y, x, Terminal.WIDTH);
                    for (int iy = terminal.y + 1; iy < Terminal.HEIGHT; iy++) {
                        terminal.bufferManager.clearLine(iy);
                    }
                }
                case 1 -> { // From beginning of screen to cursor
                    for (int iy = 0; iy < terminal.y; iy++) {
                        terminal.bufferManager.clearLine(iy);
                    }
                    terminal.bufferManager.clearLine(terminal.y, 0, x + 1);
                }
                case 2 -> // Entire screen
                        terminal.bufferManager.clear();
                default -> {}
            }
        }
    }
}