package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class CH11 extends CSISequenceHandler { // Combined Handler 11 (ICH and SL)
    public CH11(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        int chars = Math.max(args[0], 1);
        if (state.space) { // SL — Scroll Left
            // Shift all lines in scroll region left by chars, blanks fill right edge.
            chars = Math.min(chars, Terminal.WIDTH);
            for (int i = terminal.scrollFirst; i <= terminal.scrollLast; i++) {
                terminal.bufferManager.deleteChars(i, 0, chars);
            }
        } else { // ICH — Insert Character
            // Shift chars right from cursor, blanks fill at cursor.
            terminal.bufferManager.insertChars(terminal.y, terminal.x, chars);
        }
    }
}
