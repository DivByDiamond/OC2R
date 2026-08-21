package li.cil.oc2.common.vm.terminal.escapes;

import li.cil.oc2.common.vm.terminal.Terminal;

public class HTS {
    public static void execute(Terminal terminal) {
        if (terminal.x >= 0 && terminal.x < terminal.width) {
            if (terminal.currentPrivateModeState.isAltBufferEnabled())
                terminal.altTabs[terminal.x] = true;
            else terminal.tabs[terminal.x] = true;
        }
    }
}