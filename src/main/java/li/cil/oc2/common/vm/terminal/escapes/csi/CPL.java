package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * CPL — Cursor Previous Line ({@code CSI Ps F}). Moves the cursor up {@code Ps} lines (default 1)
 * and to column 0. The mirror of CNL; xterm implements it as CursorUp + CarriageReturn.
 */
public class CPL extends CSISequenceHandler {
    public CPL(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        terminal.setClampedCursorPos(0, terminal.y - args[0]);
    }
}
