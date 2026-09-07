package li.cil.oc2.common.vm.terminal.escapes.dcs;

import li.cil.oc2.common.vm.terminal.escapes.StringSequenceHandler;

// DCS (Device Control String, ESC P ... ST) handler. Stub: no DCS sequences (e.g. sixel, ReGIS)
// are implemented yet, so payload bytes are discarded and termination is a no-op. Termination
// and abort are driven uniformly by TerminalOutput (see StringSequenceHandler); this class is the
// attachment point for future DCS support, mirroring OSCManager's shape so adding a handler later
// is a drop-in (a future DCS handler will take the Terminal in its constructor).
public class DCSManager implements StringSequenceHandler {
    @Override
    public void accumulate(final int ch) {
        // No DCS sequences implemented; discard payload.
    }

    @Override
    public void terminate(final int terminator) {
        // No DCS sequences implemented.
    }

    @Override
    public void abort() {
        // Nothing buffered; nothing to drop.
    }

    @Override
    public void reset() {
    }
}
