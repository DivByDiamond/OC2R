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
        // Clamp: EscapeUtilities.parseArgument saturates at Integer.MAX_VALUE, so a guest sending
        // e.g. `X ESC[2147483647b` would loop ~2^31 times — each a full putChar (with autowrap/
        // scroll) inside the IO lock, freezing the VM worker for minutes. Cap at one screen:
        // repeating more than terminal.width * HEIGHT just scrolls off (CH8 clamps the same way).
        for (int i = 0; i < Math.min(args[0], terminal.width * Terminal.HEIGHT); i++) {
            terminal.bufferWriter.putChar(ch);
        }
    }
}
