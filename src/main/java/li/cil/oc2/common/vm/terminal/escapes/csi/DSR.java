package li.cil.oc2.common.vm.terminal.escapes.csi;

import java.util.Locale;
import li.cil.oc2.common.vm.terminal.Terminal;

public class DSR extends CSISequenceHandler {
    public DSR(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        // Ps omitted (or 0) defaults to 5 — device status report. CSIManager substitutes
        // this before execute, so a bare CSI n replies \033[0n instead of hanging the guest
        // waiting for a status response that never comes (§36 m4).
        return new int[] {5};
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (args[0] == 5) {
            if (state.questionMark) {
                /* DECDSR — DEC Device Status Report */
                terminal.io.putResponse("\033[?0n");
            } else {
                /* DSR — Standard Device Status Report */
                terminal.io.putResponse("\033[0n");
            }
        } else if (args[0] == 6) {
            if (state.questionMark) {
                /* DECXCPR — Extended Cursor Position Report (with page number) */
                terminal.io.putResponse(
                        String.format(Locale.ROOT, "\033[?%d;%d;1R", terminal.y + 1, terminal.x + 1));
            } else {
                /* DSR-CPR — Standard Cursor Position Report */
                terminal.io.putResponse(
                        String.format(Locale.ROOT, "\033[%d;%dR", terminal.y + 1, terminal.x + 1));
            }
        }
    }
}
