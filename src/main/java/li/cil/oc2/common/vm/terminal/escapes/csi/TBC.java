package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.Terminal;

public class TBC extends CSISequenceHandler {
    public TBC(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        boolean useAltBuffer = terminal.currentPrivateModeState.isAltBufferEnabled();
        if (args[0] == 0) {
            // Clear tab at current column
            if (terminal.x >= 0 && terminal.x < Terminal.WIDTH) {
                if (useAltBuffer)
                    terminal.altTabs[terminal.x] = false;
                else
                    terminal.tabs[terminal.x] = false;
            }
        } else if (args[0] == 3) {
            // Clear all tabs
            if (useAltBuffer)
                Arrays.fill(terminal.altTabs, false);
            else
                Arrays.fill(terminal.tabs, false);
        }
    }
}