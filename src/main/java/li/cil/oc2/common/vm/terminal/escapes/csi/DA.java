package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DA extends CSISequenceHandler {
    public DA(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(int[] args, int argCount, CSIState state) {
        if (state.greaterThan) {
            /* DA2 — Secondary Device Attributes.
               Pp=61 (VT510 family), Pv=10 (version 1.0),
               Pc=20993 capability bitmask:
                 1 = 132-columns (DECCOLM)
                 512 = terminal state interrogation
                 4096 = ANSI color
                 16384 = ANSI text locator (DEC Locator mode, WIP by Loki)
               DA1 remains VT100-style (?1;2c) for legacy software.
               DA2 carries the modern capability report. */
            terminal.io.putResponse("\033[>61;10;20993c");
        } else {
            /* DA1 — Primary Device Attributes. VT100 with Advanced Video Option (AVO) */
            terminal.io.putResponse("\033[?1;2c");
        }
    }
}
