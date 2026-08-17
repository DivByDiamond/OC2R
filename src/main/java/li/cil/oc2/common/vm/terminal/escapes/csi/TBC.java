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
            int x = Math.min(terminal.x, Terminal.WIDTH - 1);
            if (x >= 0) {
                if (useAltBuffer)
                    terminal.altTabs[x] = false;
                else
                    terminal.tabs[x] = false;
            }
        } else if (args[0] == 3) {
            // Clear all tabs
            Arrays.fill(terminal.tabs, false);
            Arrays.fill(terminal.altTabs, false);
        }
    }
}