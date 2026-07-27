package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DSR extends CSISequenceHandler {
    public DSR(final Terminal terminal) {
        super(terminal);
    }

    public void execute(int[] args, int argCount, CSIState state) {
        switch (args[0]) {
            case 5 -> terminal.putResponse("\033[0n"); // Report console status
            case 6 ->
                    terminal.putResponse(
                            String.format(
                                    "\033[?%d;%dR",
                                    terminal.y + 1, terminal.x + 1)); // Report cursor position
        }
    }
}