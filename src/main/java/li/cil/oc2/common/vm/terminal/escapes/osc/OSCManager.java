package li.cil.oc2.common.vm.terminal.escapes.osc;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalColors;

public class OSCManager {
    private final Terminal terminal;
    private int lastChar = '\0';

    public OSCManager(Terminal terminal) {
        this.terminal = terminal;
    }

    public void handle(int ch) {
        if ((lastChar == '\033' && ch == '\\') || ch == '\007') {
            terminal.state = Terminal.State.NORMAL;
        } else {
            lastChar = ch;
        }
    }

    public void reset() {
        lastChar = '\0';
    }
}
