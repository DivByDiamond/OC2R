package li.cil.oc2.common.vm.terminal.escapes.osc;

import li.cil.oc2.common.vm.terminal.Terminal;

// OSC 4;Ps;Pt — set (Pt = rgb:rr/gg/bb) or query (Pt = ?) palette entry Ps (0-255). The
// per-instance palette lives on Terminal.palette256, so a redefined color never bleeds across
// terminals. A single OSC 4 may carry several index;spec pairs (c;spec;c;spec;...), as xterm's
// ChangeAnsiColorRequest (misc.c:2981) does — the loop stops on the first malformed entry.
// Replies use OSC framing (ESC ] ... terminator), mirroring the query's own terminator
// (BEL or ST), and report 16-bit per channel (rgb:%04x/%04x/%04x) as xterm does (misc.c:2647).
class OSC4 extends OSCHandler {
    OSC4(Terminal terminal) {
        super(terminal);
    }

    @Override
    public void execute(final String payload, final char terminator) {
        // payload = "Ps;Pt[;Ps;Pt]..." (everything after "4;"). Walk index;spec pairs; a bare
        // "Ps" with no Pt is a no-op (query requires an explicit "?").
        final String[] parts = payload.split(";", -1);
        for (int i = 0; i + 1 < parts.length; i += 2) {
            final int ps = OSCParse.parseClampIndex(parts[i]);
            if (ps < 0) {
                return; // quit on any error, matching xterm's ChangeAnsiColorRequest.
            }
            final String pt = parts[i + 1];
            if ("?".equals(pt)) {
                replyQuery(ps, terminator);
            } else {
                final Integer rgb = OSCParse.parseRgbSpec(pt);
                if (rgb == null) {
                    return; // malformed spec -> stop on any error, matching xterm's ChangeAnsiColorRequest.
                }
                terminal.palette256[ps] = rgb;
                terminal.markPaletteDirty();
            }
        }
    }

    // Reply with the current entry as OSC 4;Ps;rgb:rrrr/gggg/bbbb <terminator>. xterm reports
    // 16-bit per channel (XColor.red is 16-bit; an 8-bit palette value is duplicated into the
    // high and low bytes), and reuses the query's own terminator (BEL or ST).
    private void replyQuery(final int ps, final char terminator) {
        final int rgb = terminal.palette256[ps];
        final int r16 = ((rgb >> 16) & 0xFF) * 0x0101; // duplicate 8-bit value into 16-bit
        final int g16 = ((rgb >> 8) & 0xFF) * 0x0101;
        final int b16 = (rgb & 0xFF) * 0x0101;
        final String term = terminator == '\007' ? "\007" : "\033\\";
        terminal.io.putResponse(String.format(
                "\033]4;%d;rgb:%04x/%04x/%04x%s", ps, r16, g16, b16, term));
    }
}
