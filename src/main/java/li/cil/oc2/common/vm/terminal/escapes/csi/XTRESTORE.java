package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.modes.ModeTable;

/**
 * Shared XTRESTORE body ({@code CSI ? Ps r} and {@code CSI ? Ps u} both restore private mode
 * {@code Ps} from the saved state) — one implementation so the two dispatch sites cannot drift
 * the way they did: CH1 marked the screen dirty when restoring DECSCNM while CH12 silently
 * flipped it, leaving a reverse-video change unrepainted. Same principle as
 * {@link SavedCursor}: the cursor save/restore families share one body so DECRC/SCORC cannot
 * diverge; the mode restore families share one body for the same reason.
 */
public final class XTRESTORE {
    private XTRESTORE() {}

    public static void execute(final Terminal terminal, final int mode) {
        final ModeTable table = ModeTable.forPrivateMode(mode);
        if (table != null) {
            table.set(terminal.currentPrivateModeState, table.get(terminal.savePrivateModeState));
            // DECSCNM (reverse video) affects the whole viewport, so restoring it must trigger a
            // full redraw — matching DECSET/DECRST (CH2/CH3), which mark the whole screen dirty.
            if (table == ModeTable.DECSCNM) {
                terminal.markAllDirty();
            }
            // DECCOLM restore previously flipped only the flag, leaving the buffer at its prior
            // width — xterm routes mode restore through the same DECSET/DECRST update path, which
            // performs the (destructive, per VT100-VT420 spec) resize. Matching CH2/CH3's DECCOLM
            // actions: reset rendition before the resize so the screen erases to the default
            // background, then resize to the restored flag's column count.
            if (table == ModeTable.DECCOLM) {
                terminal.resetRendition();
                terminal.setWidth(terminal.currentPrivateModeState.DECCOLM ? 132 : Terminal.WIDTH);
            }
        }
    }
}
