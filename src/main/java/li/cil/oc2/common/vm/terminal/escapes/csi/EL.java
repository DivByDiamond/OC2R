package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class EL extends CSISequenceHandler {
    public EL(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        int x = Math.min(terminal.x, Terminal.WIDTH - 1);
        switch (args[0]) {
            case 0 -> // From cursor to end of line
                    terminal.bufferManager.clearLine(terminal.y, x, Terminal.WIDTH);
            case 1 -> // From beginning of line to cursor
                    terminal.bufferManager.clearLine(terminal.y, 0, x + 1);
            case 2 -> // Entire line containing cursor
                    terminal.bufferManager.clearLine(terminal.y);
            default -> {}
        }
    }
}