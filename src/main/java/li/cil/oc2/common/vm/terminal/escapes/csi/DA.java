package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DA extends CSISequenceHandler {
    public DA(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (state.greaterThan) {
            /* DA2 — Secondary Device Attributes. Pp=61 (VT100 Family), Pv=10 (version 1.0), Pc=22 (color capability) */
            terminal.io.putResponse("\033[>61;10;22c");
        } else {
            /* DA1 — Primary Device Attributes. VT100 with Advanced Video Option (AVO) */
            terminal.io.putResponse("\033[?1;2c");
        }
    }
}
