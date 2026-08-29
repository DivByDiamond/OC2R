package li.cil.oc2.common.vm.terminal.escapes.osc;

import li.cil.oc2.common.vm.terminal.Terminal;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;

// OSC 104;Ps;... — reset palette entries to their defaults. No Ps resets the whole palette.
// OSC 104 is a set/reset, not a query — no reply. Multiple indices may follow (OSC 104;Ps1;Ps2).
class OSC104 extends OSCHandler {
    OSC104(Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final String payload, final char terminator) {
        if (payload.isEmpty()) {
            // No Ps -> reset the whole palette to defaults.
            terminal.palette256 = TerminalColors.getDefaultPalette256();
            terminal.markPaletteDirty();
            return;
        }
        final int[] defaults = TerminalColors.getDefaultPalette256();
        final String[] parts = payload.split(";", -1);
        boolean changed = false;
        for (final String part : parts) {
            final int ps = OSCParse.parseClampIndex(part);
            if (ps >= 0) {
                terminal.palette256[ps] = defaults[ps];
                changed = true;
            }
        }
        if (changed) {
            terminal.markPaletteDirty();
        }
    }
}
