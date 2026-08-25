package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class DECSC {
    public static void execute(Terminal terminal) {
        SavedCursor.save(terminal);
    }
}
