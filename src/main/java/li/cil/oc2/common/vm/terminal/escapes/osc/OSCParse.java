package li.cil.oc2.common.vm.terminal.escapes.osc;

// Shared parsing helpers for OSC handlers. Kept in one place because OSC 4 (set) and the
// future OSC 10/11 (default fg/bg) both speak the same rgb:rr/gg/bb spec, and the palette
// index clamp is common to OSC 4 and OSC 104.
final class OSCParse {
    private OSCParse() {
    }

    // Parse a non-negative OSC code (unbounded — OSC 777/1337 must dispatch, not die at the
    // 0-255 palette edge). -1 if non-numeric or negative. Used for the routing code only;
    // palette entries use parseClampIndex.
    static int parseCode(final String s) {
        try {
            final int v = Integer.parseInt(s);
            return v >= 0 ? v : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    // Parse a non-negative int in 0-255 (a palette index); -1 if non-numeric or out of range.
    // xterm ignores malformed OSC rather than erroring, so callers treat -1 as a silent no-op.
    static int parseClampIndex(final String s) {
        try {
            final int v = Integer.parseInt(s);
            return (v >= 0 && v <= 255) ? v : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    // Parse "rgb:rr/gg/bb" (2 hex digits per channel) into a packed 0xRRGGBB int; null if
    // malformed. #rrggbb shorthand and 4-/16-bit rgb variants are out of scope (xterm extends
    // to those; the palette is 8-bit per channel so 2 digits suffice).
    static Integer parseRgbSpec(final String pt) {
        if (!pt.startsWith("rgb:")) {
            return null;
        }
        final String[] channels = pt.substring(4).split("/", -1);
        if (channels.length != 3) {
            return null;
        }
        try {
            final int r = Integer.parseInt(channels[0], 16) & 0xFF;
            final int g = Integer.parseInt(channels[1], 16) & 0xFF;
            final int b = Integer.parseInt(channels[2], 16) & 0xFF;
            return (r << 16) | (g << 8) | b;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
