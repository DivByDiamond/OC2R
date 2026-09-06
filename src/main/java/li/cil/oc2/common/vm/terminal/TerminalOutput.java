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
import li.cil.oc2.common.vm.terminal.escapes.StringSequenceHandler;
import li.cil.oc2.common.vm.terminal.escapes.index.IND;
import li.cil.oc2.common.vm.terminal.escapes.index.NEL;
import li.cil.oc2.common.vm.terminal.escapes.index.RI;
import li.cil.oc2.common.vm.terminal.escapes.index.RIS;
import li.cil.oc2.common.vm.terminal.modes.impl.KeypadMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class TerminalOutput { // NOPMD CyclomaticComplexity: dense VT100 state-machine dispatch

    private static final Logger LOGGER = LogManager.getLogger();

    private final ReentrantLock lock;

    private final Terminal terminal;
    private final Utf8Decoder decoder = new Utf8Decoder();

    // True between an ESC seen mid-string (OSC/DCS/APC) and the next byte, while we wait to find
    // out whether it is '\' (completing ST) or the start of a fresh escape. Owned here, not in the
    // string managers, so termination is uniform across all three (xterm's single sos_table).
    private boolean stringEscapePending = false;

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
            case DCS -> handleStringByte(ch, terminal.dcsManager, false);
            case OSC -> handleStringByte(ch, terminal.oscManager, true);
            case APC -> handleStringByte(ch, terminal.apcManager, false);
            default -> {
                // Exhaustive over the known states; guards against future additions.
            }
        }
    }

    // Drives an ST-terminated string state (OSC/DCS/APC). Termination is owned here, uniformly for
    // all three, mirroring xterm's single sos_table (VTPrsTbl.c): ESC arms a potential ST, '\'
    // completes it, BEL ends OSC only, CAN/SUB abort. Content bytes (including '\' not preceded by
    // ESC) go to the handler's accumulate. On ESC + another byte, xterm leaves esc_table and the
    // result depends on that byte: '\' completes ST; BEL (CASE_BELL), CAN/SUB (CASE_CAN/CASE_SUB)
    // are handled above; a fresh escape introducer ([, P, ], _...) or escape final drops the held
    // string and begins a new sequence (the parsestate != esc_table check at charproc.c:3478 clears
    // string_used). OC2R's pending branch mirrors the drop-and-begin for those bytes; for the C0
    // controls xterm would instead execute-and-hold (CASE_BS/TAB/VMOT/CR/SO/SI run the control and
    // keep the string in esc_table), OC2R aborts-and-redispatches — a stricter, griefer-safe
    // divergence (a control interleaved into an OSC payload is malformed; dropping it can't be
    // abused). Re-dispatch is via handleEscape, so e.g. ESC [ mid-OSC starts a CSI rather than
    // buffering '[' as payload. (For a nested string start ESC P/] xterm's BeginString appends to
    // the old payload instead of clearing — OC2R's abort-and-start-fresh is the saner divergence.)
    private void handleStringByte(final char ch, final StringSequenceHandler handler, // NOPMD VT100 string-state byte dispatch; each branch is required
                                  final boolean belTerminates) {
        if (stringEscapePending) {
            if (ch == '\\') {
                // ST: ESC \ completes the string terminator.
                stringEscapePending = false;
                handler.terminate('\\');
                terminal.state = State.NORMAL;
            } else if (ch == '\007') {
                // ESC BEL: xterm CASE_BELL. The ESC moved to esc_table with the string held; for
                // OSC BEL terminates and processes the payload (charproc.c:3687); for DCS/APC it
                // rings and keeps the string held in esc_table (charproc.c:3697) — so stay pending:
                // the ESC is still unresolved, the next byte resolves it.
                if (belTerminates) {
                    stringEscapePending = false;
                    handler.terminate('\007');
                    terminal.state = State.NORMAL;
                } else {
                    terminal.hasPendingBell = true;
                }
            } else if (ch == '\030' || ch == '\032') {
                // ESC CAN / ESC SUB: xterm CASE_CAN/CASE_SUB abort to ground. CAN/SUB are cancel
                // bytes, not escape introducers, so abort directly — no re-dispatch, which would
                // otherwise log a spurious "Invalid escape" warning for a legitimate abort byte.
                stringEscapePending = false;
                handler.abort();
                terminal.state = State.NORMAL;
            } else if (ch != '\033') {
                // ESC + an escape introducer/final or a C0 control: drop the held string and
                // re-dispatch the byte through handleEscape (so e.g. ESC [ mid-OSC starts a CSI,
                // not a buffered '['). For escape introducers/finals this matches xterm's
                // drop-and-begin; for C0 controls (BS/CR/LF/...) xterm would execute-and-hold —
                // OC2R aborts instead (stricter, griefer-safe; see method doc). ESC ENQ hits the
                // "Invalid escape" warn here — pathological, accepted.
                stringEscapePending = false;
                handler.abort();
                handleEscape(ch);
            }
            // else ESC ESC: xterm re-enters esc_table with the string held (charproc.c CASE_ESC).
            // Re-arm (stringEscapePending stays true) and keep waiting — the payload is processed
            // only if '\' next follows, dropped otherwise. ESCs themselves do not accumulate.
            return;
        }
        if (ch == '\033') {
            stringEscapePending = true;
            return;
        }
        if (ch == '\007') {
            if (belTerminates) {
                handler.terminate('\007');
                terminal.state = State.NORMAL;
            } else {
                // BEL in a non-OSC string rings the bell (xterm CASE_BELL, non-OSC path) without
                // terminating — DCS/APC continue.
                terminal.hasPendingBell = true;
            }
            return;
        }
        if (ch == '\030' || ch == '\032') { // CAN / SUB: abort the string, return to ground.
            handler.abort();
            terminal.state = State.NORMAL;
            return;
        }
        handler.accumulate(ch);
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
                stringEscapePending = false;
                terminal.state = State.DCS;
            }
            case ']' -> {
                terminal.oscManager.reset();
                stringEscapePending = false;
                terminal.state = State.OSC;
            }
            case '_' -> {
                terminal.apcManager.reset();
                stringEscapePending = false;
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
