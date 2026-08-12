package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * DECREQTPARM — Request Terminal Parameters.
 * ESC [ {@code <sol>} x
 * Response (DECREPTPARM): ESC [ 3; {@code <par>}; {@code <nbits>}; {@code <xspeed>}; {@code <rspeed>}; {@code <clkmul>}; {@code <flags>} x
 * {@code <sol>}=3 means report, only on request.
 * We report: no parity (1), 8 bits (1), 9600 baud (112), 9600 baud (112), clk mul 1, flags 0.
 */
public class DECREQTPARM extends CSISequenceHandler {
    public DECREQTPARM(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        int sol = (argCount > 0 && args[0] == 1) ? 3 : 2;
        terminal.io.putResponse(String.format("\033[%d;1;1;112;112;1;0x", sol));
    }
}
