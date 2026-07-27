package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class ED extends CSISequenceHandler {
    public ED(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (state.questionMark) {
            System.out.println("DECSED is not implemented");
        } else {
            switch (args[0]) {
                case 0 -> { // From cursor to end of screen
                    terminal.clearLine(terminal.y, terminal.x, Terminal.WIDTH);
                    for (int iy = terminal.y + 1; iy < Terminal.HEIGHT; iy++) {
                        terminal.clearLine(iy);
                    }
                }
                case 1 -> { // From beginning of screen to cursor
                    for (int iy = 0; iy < terminal.y; iy++) {
                        terminal.clearLine(iy);
                    }
                    terminal.clearLine(terminal.y, 0, terminal.x + 1);
                }
                case 2 -> // Entire screen
                        terminal.clear();
                default -> {}
            }
        }
    }
}