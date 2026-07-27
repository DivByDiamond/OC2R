package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DSR extends CSISequenceHandler {
    public DSR(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (args[0] == 5) {
            terminal.io.putResponse("\033[0n"); // Report console status
        } else if (args[0] == 6) {
            terminal.io.putResponse(
                    String.format(
                            "\033[?%d;%dR",
                            terminal.y + 1, terminal.x + 1)); // Report cursor position
        }
    }
}