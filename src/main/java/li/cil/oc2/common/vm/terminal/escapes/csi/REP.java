package li.cil.oc2.common.vm.terminal.escapes.csi;

import li.cil.oc2.common.vm.terminal.Terminal;

/**
 * REP — Repeat ({@code CSI Ps b}). Repeats the preceding printable character {@code Ps} times
 * (default 1) by feeding it back through {@link TerminalBufferWriter#putChar}, so the repeats
 * honor autowrap, insert mode, and color exactly like the original. If no graphic character has
 * been printed since the last cursor move (xterm's {@code lastchar == -1}), REP is a no-op.
 */
public class REP extends CSISequenceHandler {
    public REP(final Terminal terminal) {
        super(terminal);
    }

    @Override
    public int[] defaultParameters(CSIState state) {
        return new int[] {1};
    }

    @Override
    public void execute(final int[] args, final int argsCount, final CSIState state) {
        final int ch = terminal.lastPrintedChar;
        if (ch < 0) {
            return; // no preceding graphic char to repeat (xterm: lastchar == -1)
        }
        for (int i = 0; i < args[0]; i++) {
            terminal.bufferWriter.putChar(ch);
        }
    }
}
