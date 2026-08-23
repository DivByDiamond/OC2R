package li.cil.oc2.common.util.tick;

import java.util.function.Consumer;
import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.TerminalDiff;
import li.cil.oc2.common.vm.terminal.escapes.index.RIS;

public final class TerminalUtils {
    /**
     * Resets the server-side terminal (RIS) and ships a full-screen snapshot flagged as
     * reset to clients, so their local display copies start from a clean, consistent state.
     */
    public static void resetTerminal(
            final Terminal terminal, final Consumer<TerminalDiff.Snapshot> packetSender) {
        RIS.execute(terminal);
        packetSender.accept(TerminalDiff.captureFull(terminal));
    }
}