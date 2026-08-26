package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * VPR — Vertical Position Relative ({@code CSI Ps e}). Moves the cursor down {@code Ps} rows
 * (default 1), keeping the column. The relative mirror of VPA.
 */
public class VPR extends CSISequenceHandler {
    public VPR(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        // Down by Ps rows (relative). Bounded relative move (see Terminal.moveCursorBy): a
        // saturated CSI count can't overflow the int sum before setClampedCursorPos clamps.
        terminal.moveCursorBy(0, args[0]);
    }
}
