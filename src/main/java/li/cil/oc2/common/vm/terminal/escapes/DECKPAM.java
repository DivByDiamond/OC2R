package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECKPAM {
    public static void execute(Terminal terminal) {
        terminal.currentPrivateModeState.DECNKM = true;
    }
}
