package li.cil.oc2.common.vm.terminal.escapes;

// Shared contract for the ST-terminated string sequences (OSC, DCS, APC). TerminalOutput owns
// the termination logic — ESC (the ST introducer), BEL, CAN/SUB — uniformly for all three,
// mirroring xterm's single sos_table (VTPrsTbl.c): one place decides how a string ends, so the
// abort/terminate behavior is consistent across OSC/DCS/APC instead of reimplemented (and
// diverging) in each manager. Each manager owns only what is specific to its sequence type:
// buffering the payload and, on normal termination, routing it.
//
// Bytes reach the manager only AFTER TerminalOutput has ruled out a termination/abort byte.
// A content byte — including '\', which is content unless it follows ESC — is passed to
// accumulate. On ST (ESC \) or BEL the manager is told to terminate; on abort (ESC not followed
// by '\', or CAN/SUB) it is told to abort. xterm drops the accumulated payload on abort rather
// than processing it (charproc.c: CASE_ESC -> esc_table, then the parsestate != esc_table clear
// at charproc.c:3478 discards string_used); terminate/abort reflect that distinction.
public interface StringSequenceHandler {
    // Buffer a content byte. Implementations bound the buffer so a stream that never terminates
    // cannot grow unbounded (xterm caps similarly via strings_max).
    void accumulate(int ch);

    // The string ended normally (ST or BEL). Process the accumulated payload. {@code terminator}
    // is the byte that ended it — BEL (0x07) or the backslash of ST (0x5C) — so a replying
    // handler can mirror the query's framing (xterm unparseputc1(xw, final), misc.c:2655).
    void terminate(int terminator);

    // The string was aborted (ESC + non-'\', or CAN/SUB). Drop the payload without processing.
    void abort();

    // Clear all state. Called when entering the string state, so a fresh sequence begins clean.
    void reset();
}
