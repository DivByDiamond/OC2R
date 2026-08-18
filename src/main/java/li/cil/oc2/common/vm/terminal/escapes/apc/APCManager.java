package li.cil.oc2.common.vm.terminal.escapes.apc;

import li.cil.oc2.common.vm.terminal.Terminal;

public class APCManager {
    private final Terminal terminal;
    private int lastChar = '\0';

    public APCManager(Terminal terminal) {
        this.terminal = terminal;
    }

    public void handle(int ch) {
        if (lastChar == '\033' && ch == '\\') {
            terminal.state = Terminal.State.NORMAL;
        } else {
            lastChar = ch;
        }
    }

    public void reset() {
        lastChar = '\0';
    }
}