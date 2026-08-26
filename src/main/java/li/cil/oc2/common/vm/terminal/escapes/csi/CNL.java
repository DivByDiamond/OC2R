package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * CNL — Cursor Next Line ({@code CSI Ps E}). Moves the cursor down {@code Ps} lines (default 1) and
 * to column 0. The {@code CSI E} form of NEL; xterm implements it as CursorDown + CarriageReturn.
 */
public class CNL extends CSISequenceHandler {
    public CNL(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        terminal.setClampedCursorPos(0, terminal.y + args[0]);
    }
}
