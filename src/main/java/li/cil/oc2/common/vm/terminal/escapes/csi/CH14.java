package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * Combined Handler 14 (CUU and SR) — the {@code A} final is shared between modifiers:
 * plain CSI is CUU (Cursor Up), while the SP intermediate is SR (Scroll Right) — xterm-410
 * {@code csi_sp_table} (VTPrsTbl.c) maps SP {@code A} to {@code CASE_SR}, not a cursor move.
 * Dispatching SP-modified {@code A} into CUU would move the cursor where xterm scrolls.
 */
public class CH14 extends CSISequenceHandler {
    public CH14(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        if (state.space) { // SR — Scroll Right: shift each scroll-region row right, blank the left
            // xterm-410 xtermScrollLR/xtermColScroll (util.c): rows come from the scroll margins
            // (full screen when margins are full-page), the operation is gated on the cursor
            // being inside the row margins, and the cursor itself is saved/restored — the
            // scroll does not move it. insertChars per row shifts right and blanks the head.
            if (terminal.y < terminal.scrollFirst || terminal.y > terminal.scrollLast) {
                return;
            }
            for (int i = terminal.scrollFirst; i <= terminal.scrollLast; i++) {
                terminal.bufferManager.insertChars(i, 0, args[0]);
            }
        } else { // CUU — Cursor Up
            // Up by Ps rows. Bounded relative move (see Terminal.moveCursorBy): a saturated CSI
            // count can't overflow the int sum before setClampedCursorPos clamps.
            terminal.moveCursorBy(0, -args[0]);
        }
    }
}
