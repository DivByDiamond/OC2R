package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * HPA — Horizontal Position Absolute ({@code CSI Ps `}). Moves the cursor to column {@code Ps}
 * (default 1, 1-based), keeping the row. A sibling of CHA ({@code CSI Ps G}); xterm uses the
 * same CursorSet path, decrementing the 1-based column.
 */
public class HPA extends CSISequenceHandler {
    public HPA(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(final CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        terminal.setRelativeCursorPos(args[0] - 1, terminal.y);
    }
}
