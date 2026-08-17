package li.cil.oc2.common.vm.terminal.buffer.utf8;

public class Utf8Decoder {
    private boolean continuationByte;
    private int bytesToRead;
    private int bytesRead;
    private int codepoint;

    public boolean hasActiveSequence() {
        return continuationByte;
    }

    public int getCodepoint() {
        return codepoint;
    }

    public void sequenceProcessed() {
        continuationByte = false;
    }

    public boolean process(final byte value) {
        final int ch = value & 0xFF;
        if (!continuationByte && (ch & 0x80) != 0) {
            if ((ch & 0x40) != 0) {
                bytesRead = 0;
                continuationByte = true;
                bytesToRead++;
            } else {
                return false;
            }
            if ((ch & 0x20) != 0) {
                bytesToRead++;
            } else {
                codepoint = (ch & 0x1F) << 6;
                return false;
            }
            if ((ch & 0x10) != 0) {
                bytesToRead++;
            } else {
                codepoint = (ch & 0x0F) << 12;
                return false;
            }
            codepoint = (ch & 0x07) << 18;
            return false;
        } else if (continuationByte) {
            if ((ch & 0x80) == 0) {
                continuationByte = false;
                bytesToRead = 0;
                bytesRead = 0;
                codepoint = ch;
                return true;
            }
            bytesRead++;
            codepoint |= (ch & 0x3F) << ((bytesToRead - bytesRead) * 6);
            if (bytesToRead == bytesRead) {
                bytesToRead = 0;
                bytesRead = 0;
                return true;
            }
            return false;
        }
        codepoint = ch;
        return true;
    }
}
