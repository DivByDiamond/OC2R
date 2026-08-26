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
        // Down by Ps lines and to column 0 (the CSI E form of NEL). moveCursorBy preserves the
        // column, so this resets it explicitly and clamps the delta inline: parseArgument saturates
        // at Integer.MAX_VALUE, so terminal.y + args[0] would overflow negative to row 0 (top)
        // instead of the bottom row.
        terminal.setClampedCursorPos(0, terminal.y + Math.clamp(args[0], 0, Terminal.HEIGHT));
    }
}
