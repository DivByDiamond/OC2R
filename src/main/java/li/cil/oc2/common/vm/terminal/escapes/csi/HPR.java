package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * HPR — Horizontal Position Relative ({@code CSI Ps a}). Moves the cursor right {@code Ps} columns
 * (default 1), keeping the row. The relative mirror of HPA/CHA.
 */
public class HPR extends CSISequenceHandler {
    public HPR(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        // Right by Ps columns (relative). Bounded relative move (see Terminal.moveCursorBy): a
        // saturated CSI count can't overflow the int sum before setClampedCursorPos clamps.
        terminal.moveCursorBy(args[0], 0);
    }
}
