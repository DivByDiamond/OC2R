package li.cil.oc2.common.vm.terminal.buffer.utf8;

/** Incremental UTF-8 scalar decoder; malformed sequences are discarded and the next lead resynchronizes. */
public class Utf8Decoder {
    private int remaining;
    private int minimum;
    private int codepoint;

    public int getCodepoint() {
        return codepoint;
    }

    public void sequenceProcessed() {
        remaining = 0;
    }

    public boolean process(final byte value) {
        final int ch = value & 0xFF;
        if (remaining != 0 && (ch & 0xC0) == 0x80) {
            codepoint = (codepoint << 6) | (ch & 0x3F);
            remaining--;
            if (remaining != 0) return false;
            return codepoint >= minimum && codepoint <= 0x10FFFF
                    && (codepoint < 0xD800 || codepoint > 0xDFFF);
        }
        // A new lead or ASCII aborts an incomplete sequence, without consuming the new character.
        if (ch < 0x80) {
            remaining = 0;
            codepoint = ch;
            return true;
        }
        if (ch >= 0xC2 && ch <= 0xDF) {
            remaining = 1;
            minimum = 0x80;
            codepoint = ch & 0x1F;
        } else if (ch >= 0xE0 && ch <= 0xEF) {
            remaining = 2;
            minimum = 0x800;
            codepoint = ch & 0x0F;
        } else if (ch >= 0xF0 && ch <= 0xF4) {
            remaining = 3;
            minimum = 0x10000;
            codepoint = ch & 0x07;
        } else {
            remaining = 0;
        }
        return false;
    }
}
