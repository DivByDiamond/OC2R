package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CH11 extends CSISequenceHandler { // Combined Handler 11 (ICH and SL)
    public CH11(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.space) { // SL — Scroll-Left: shift each scroll-region row left, blank the right
            for (int i = terminal.scrollFirst; i <= terminal.scrollLast; i++) {
                terminal.bufferManager.deleteChars(i, 0, args[0]);
            }
        } else { // ICH — Insert Character: shift chars right from the cursor, blank the gap
            // insertChars clamps count to the remaining width, fills with the current
            // background / DEFAULT_FOREGROUND, and marks the rendered screen row dirty.
            terminal.bufferManager.insertChars(terminal.y, terminal.x, args[0]);
        }
    }
}
