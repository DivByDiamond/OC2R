package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;

public class TBC extends CSISequenceHandler {
    public TBC(final Terminal terminal) {
        super(terminal);
    }

    public void execute(int[] args, int argCount, CSIState state) {
        switch (args[0]) {
            case 0 -> { // Clear tab at current column
                if (terminal.x >= 0 && terminal.x < Terminal.WIDTH) {
                    terminal.tabs[terminal.x] = false;
                }
            }
            case 3 -> // Clear all tabs
                    Arrays.fill(terminal.tabs, false);
        }
    }
}