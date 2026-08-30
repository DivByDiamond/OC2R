package li.cil.oc2.common.vm.terminal.escapes.osc;

import li.cil.oc2.common.vm.terminal.Terminal;

// One OSC handler per numeric code, mirroring the CSISequenceHandler/CSIManager split: the
// manager accumulates the payload (OSC is an opaque string terminated by ST/BEL, so it is
// only parseable whole, not streaming) and routes it by code; each handler owns its own
// argument parsing. The dispatch table in OSCManager is the addable surface — a new OSC is a
// new class plus one registration, not a switch arm grown inside the manager.
public abstract class OSCHandler {
    protected final Terminal terminal;

    public OSCHandler(Terminal terminal) {
        this.terminal = terminal;
    }

    // Act on an OSC payload. {@code payload} is everything after the numeric code and its
    // separating {@code ;}; for a code with no parameters it is the empty string.
    // {@code terminator} is the byte that ended the sequence — BEL (0x07) or the backslash of
    // ST (0x5C) — so a handler that replies (e.g. OSC 4 query) can mirror the query's framing,
    // matching xterm's unparseputc1(xw, final) (misc.c:2655).
    public abstract void execute(String payload, char terminator);
}
