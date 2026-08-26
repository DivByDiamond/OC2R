package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import li.cil.oc2.common.vm.terminal.Terminal.State;
import li.cil.oc2.common.vm.terminal.buffer.utf8.Utf8Decoder;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.escapes.DECRC;
import li.cil.oc2.common.vm.terminal.escapes.DECSC;
import li.cil.oc2.common.vm.terminal.escapes.HTS;
import li.cil.oc2.common.vm.terminal.escapes.index.IND;
import li.cil.oc2.common.vm.terminal.escapes.index.NEL;
import li.cil.oc2.common.vm.terminal.escapes.index.RI;
import li.cil.oc2.common.vm.terminal.escapes.index.RIS;
import li.cil.oc2.common.vm.terminal.modes.impl.KeypadMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class TerminalOutput {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ReentrantLock lock;

    private final Terminal terminal;
    private final Utf8Decoder decoder = new Utf8Decoder();

    TerminalOutput(final Terminal terminal, final ReentrantLock lock) {
        this.terminal = terminal;
        this.lock = lock;
    }

    public void putOutput(final ByteBuffer values) {
        lock.lock();
        try {

            while (values.hasRemaining()) {
                putOutput(values.get());
            }

        } finally {
            lock.unlock();
        }
    }

    public void putOutput(final byte value) {
        lock.lock();
        try {
            if (!decoder.process(value)) {
                return;
            }
            dispatch((char) value);
        } finally {
            lock.unlock();
        }
    }

    private void dispatch(final char ch) { // NOPMD 11-case VT100 dispatch; each branch is required
        // XT_RAW_PASSTHROUGH (CSI ?7777h): built-in byte-capture debugger. While ON every byte
        // is written to the screen as a visible glyph and NO byte is interpreted — not even ESC
        // enters the state machine. Control bytes render as caret notation (^[, ^M, ^H, ...) so
        // the raw stream is readable. Because passthrough swallows escapes too, the toggle-off
        // (CSI ?7777l) is matched as a literal byte sequence here — the only sequence interpreted
        // while the mode is on — so the debugger can always be exited. Any byte that isn't part of
        // the in-progress exit sequence renders immediately; partial matches back out and render.
        if (terminal.currentPrivateModeState.XT_RAW_PASSTHROUGH) {
            if (matchExitSequence((byte) ch)) {
                return; // a complete (or partially-matched) exit sequence: don't render
            }
            renderRawByte((byte) ch);
            return;
        }
        switch (terminal.state) {
            case NORMAL -> handleNormal((byte) ch);
            case ESCAPE -> handleEscape(ch);
            case CONTROL_SEQUENCE -> terminal.csiManager.handle(ch);
            case SHIFT_IN_CHARACTER_SET, SHIFT_OUT_CHARACTER_SET -> handleShiftInShiftOut(ch);
            case HASH -> handleHash(ch);
            case DCS -> terminal.dcsManager.handle(ch);
            case OSC -> terminal.oscManager.handle(ch);
            case APC -> terminal.apcManager.handle(ch);
            default -> {
                // Exhaustive over the known states; guards against future additions.
            }
        }
    }

    // The exact toggle-off bytes: ESC [ ? 7 7 7 7 l  (CSI ? 7777 l). Matched byte-for-byte while
    // passthrough is on so the mode can always be exited; the matched bytes are consumed (not
    // rendered). A non-matching byte flushes any buffered partial match as rendered glyphs.
    private static final byte[] EXIT_SEQUENCE = {
            0x1B, '[', '?', '7', '7', '7', '7', 'l'};
    private int exitMatchPos = 0;
    private final byte[] exitMatchBuf = new byte[EXIT_SEQUENCE.length];

    private boolean matchExitSequence(final byte value) {
        if (value == EXIT_SEQUENCE[exitMatchPos]) {
            exitMatchBuf[exitMatchPos] = value;
            exitMatchPos++;
            if (exitMatchPos == EXIT_SEQUENCE.length) {
                // Complete match: clear the mode, discard the matched bytes.
                terminal.currentPrivateModeState.XT_RAW_PASSTHROUGH = false;
                exitMatchPos = 0;
            }
            return true;
        }
        // Mismatch. Flush any partial match we had buffered as rendered glyphs first, so those
        // bytes still reach the screen; then re-check this byte against the sequence start (it
        // may itself be the first byte of a new match, e.g. an ESC that ends one near-match and
        // begins another).
        final int flushed = exitMatchPos;
        exitMatchPos = 0;
        for (int i = 0; i < flushed; i++) {
            renderRawByte(exitMatchBuf[i]);
        }
        if (value == EXIT_SEQUENCE[0]) {
            exitMatchBuf[0] = value;
            exitMatchPos = 1;
            return true;
        }
        return false;
    }

    /**
     * Writes a single raw byte to the screen as a visible glyph, bypassing the control-byte drop
     * in {@link li.cil.oc2.common.vm.terminal.buffer.TerminalBufferWriter#putChar}. Used only while
     * XT_RAW_PASSTHROUGH is on. Control bytes (0x00–0x1F, 0x7F) become caret notation; printable
     * bytes pass through as themselves. Bytes >= 0x80 (UTF-8 continuations / high bytes) render
     * as a fixed placeholder so multi-byte sequences don't get half-decoded into glyphs.
     */
    private void renderRawByte(final byte value) {
        final int ch = value & 0xFF;
        if (ch < 0x20 || ch == 0x7F) {
            // Caret notation: ESC (0x1B) -> ^[, CR (0x0D) -> ^M, etc. Write '^' then the byte ^ 0x40.
            terminal.bufferWriter.putChar('^');
            terminal.bufferWriter.putChar(ch ^ 0x40);
        } else if (ch >= 0x80) {
            terminal.bufferWriter.putChar('~'); // high-byte placeholder (don't half-decode UTF-8)
        } else {
            terminal.bufferWriter.putChar(ch);
        }
    }

    private void handleNormal(final byte value) {
        switch (normalizeControl(value)) {
            case '\007' -> terminal.hasPendingBell = true;
            case '\033' -> terminal.state = State.ESCAPE;
            case '\016' -> terminal.useG0 = false;
            case '\017' -> terminal.useG0 = true;

            case (byte) '\r' -> terminal.setCursorPos(0, terminal.y);
            case (byte) '\n' -> handleLineFeed();
            case (byte) '\t' -> handleTab();
            case (byte) '\b' ->
                    terminal.setCursorPos(Math.max(0, terminal.x - 1), terminal.y);

            default -> {
                terminal.bufferWriter.putChar(decoder.getCodepoint());
                decoder.sequenceProcessed();
            }
        }
    }

    private static byte normalizeControl(final byte value) {
        return value == 0x0B || value == 0x0C ? (byte) '\n' : value;
    }

    private void handleLineFeed() {
        if (terminal.currentModeState.LNM) {
            NEL.execute(terminal);
        } else {
            IND.execute(terminal);
        }
    }

    private void handleTab() {
        terminal.autowrapPending = false; // Tab is a cursor move — clears the pending wrap (xterm ResetWrap)
        if (terminal.x < terminal.width - 1) {
            do {
                terminal.x++;
            } while (terminal.x < terminal.width - 1
                    && (terminal.currentPrivateModeState.isAltBufferEnabled()
                            ? !terminal.altTabs[terminal.x]
                            : !terminal.tabs[terminal.x]));
        }
    }

    private void handleEscape(final char ch) {
        switch (ch) {
            case '[' -> {
                terminal.csiManager.reset();
                terminal.state = State.CONTROL_SEQUENCE;
            }
            case '(' -> terminal.state = State.SHIFT_IN_CHARACTER_SET;
            case ')' -> terminal.state = State.SHIFT_OUT_CHARACTER_SET;
            case '#' -> terminal.state = State.HASH;
            case 'P' -> {
                terminal.dcsManager.reset();
                terminal.state = State.DCS;
            }
            case ']' -> {
                terminal.oscManager.reset();
                terminal.state = State.OSC;
            }
            case '_' -> {
                terminal.apcManager.reset();
                terminal.state = State.APC;
            }
            default -> handleSingleCharEscape(ch);
        }
    }

    private void handleSingleCharEscape(final char ch) { // NOPMD 10-case VT100 escape dispatch
        terminal.state = State.NORMAL;
        switch (ch) {
            case 'D' -> IND.execute(terminal);
            case 'E' -> NEL.execute(terminal);
            case 'M' -> RI.execute(terminal);
            case '7' -> DECSC.execute(terminal);
            case '8' -> DECRC.execute(terminal);
            case 'H' -> HTS.execute(terminal);
            case 'c' -> RIS.execute(terminal);
            case '=' -> KeypadMode.setApplication(terminal);
            case '>' -> KeypadMode.setNumeric(terminal);
            default -> LOGGER.warn("Invalid escape: {}", ch);
        }
    }

    private void handleShiftInShiftOut(final char ch) {
        terminal.state = State.NORMAL;
        switch (ch) {
            case 'A' -> {}
            case 'B' -> terminal.drawingModeG0 = TerminalColors.DrawingMode.ASCII;
            case '0' ->
                    terminal.drawingModeG0 =
                            TerminalColors.DrawingMode.SPECIAL_GRAPHICS;
            case '1' -> {}
            case '2' -> {}
            default -> {}
        }
    }

    private void handleHash(final char ch) {
        terminal.state = State.NORMAL;
        if (ch == '8') {
            if (terminal.currentPrivateModeState.isAltBufferEnabled()) {
                Arrays.fill(terminal.altBuffer, 'E');
            } else {
                int startIndex =
                        (terminal.lastRowToDisplayMax - Terminal.HEIGHT)
                                * terminal.width;
                Arrays.fill(
                        terminal.buffer,
                        startIndex,
                        startIndex + terminal.width * Terminal.HEIGHT,
                        'E');
            }
            terminal.markAllDirty();
        }
    }
}
