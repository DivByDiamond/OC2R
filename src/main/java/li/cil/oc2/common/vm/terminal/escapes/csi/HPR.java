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
    public int[] defaultParameters(final CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        terminal.setClampedCursorPos(terminal.x + args[0], terminal.y);
    }
}
