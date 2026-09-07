package li.cil.oc2.common.vm.terminal.escapes.apc;

import li.cil.oc2.common.vm.terminal.escapes.StringSequenceHandler;

// APC (Application Program Command, ESC _ ... ST) handler. Stub: xterm ignores APC entirely
// (charproc.c CASE_ST, ANSI_APC -> "ignored"), and OC2R has no APC use either, so payload bytes
// are discarded and termination is a no-op. Termination and abort are driven uniformly by
// TerminalOutput (see StringSequenceHandler); this class exists for symmetry with OSC/DCS and as
// the attachment point should an APC sequence ever need handling.
public class APCManager implements StringSequenceHandler {
    @Override
    public void accumulate(final int ch) {
        // APC is ignored; discard payload.
    }

    @Override
    public void terminate(final int terminator) {
        // APC is ignored.
    }

    @Override
    public void abort() {
        // Nothing buffered; nothing to drop.
    }

    @Override
    public void reset() {
    }
}
