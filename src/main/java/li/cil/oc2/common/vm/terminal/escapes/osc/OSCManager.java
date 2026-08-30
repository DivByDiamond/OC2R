package li.cil.oc2.common.vm.terminal.escapes.osc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.common.vm.terminal.Terminal;

// OSC (Operating System Command, ESC ] ... ST) dispatch. Bytes arrive one at a time from
// TerminalOutput once the state machine enters the OSC state; this accumulates the payload
// and, on the ST (ESC \) or BEL terminator, routes it by numeric code to a per-code handler.
//
// OSC is an opaque string terminated by ST/BEL, so — unlike CSI — it is only parseable whole,
// not streaming: the manager buffers the full payload and hands it to the handler on
// termination (xterm accumulates the same way). The handler-per-code split mirrors
// CSISequenceHandler/CSIManager: a new OSC is a new OSCHandler subclass plus one registration,
// not a switch arm grown inside the manager. Implemented: OSC 4 (set/query palette entry) and
// OSC 104 (reset palette entries); the table leaves OSC 0/2/8/10/11 as addable follow-ups.
public class OSCManager {
    // Bounds the payload buffer so a stream that never terminates can't grow unbounded; 1024
    // is well past any OSC 4/104 payload (a set is ~20 chars). xterm caps similarly. A char[]
    // (not a StringBuilder field) is used because OSCManager lives as long as its Terminal
    // (PMD AvoidStringBufferField).
    private static final int BUFFER_CAP = 1024;
    private final Terminal terminal;
    private final Map<Integer, OSCHandler> handlers = new ConcurrentHashMap<>();
    private final char[] buffer = new char[BUFFER_CAP];
    private int bufferLength = 0;
    private int lastChar = '\0';

    public OSCManager(Terminal terminal) {
        this.terminal = terminal;
        handlers.put(4, new OSC4(terminal));
        handlers.put(104, new OSC104(terminal));
    }

    public void handle(int ch) {
        // ST terminator: ESC followed by '\'. BEL terminator: 0x07. The ESC that begins ST is
        // armed via lastChar (below) but never buffered, so the payload stays clean. The final
        // byte (BEL 0x07, or the '\' of ST 0x5C) is passed to processSequence so a replying
        // handler can mirror the query's framing — xterm's unparseputc1(xw, final) (misc.c:2655).
        if ((lastChar == '\033' && ch == '\\') || ch == '\007') {
            processSequence(new String(buffer, 0, bufferLength), (char) ch);
            bufferLength = 0;
            lastChar = '\0';
            terminal.state = Terminal.State.NORMAL;
            return;
        }
        if (ch == '\033') {
            // Start of the ST terminator — arm lastChar, don't buffer it.
            lastChar = ch;
            return;
        }
        if (bufferLength < BUFFER_CAP) {
            buffer[bufferLength++] = (char) ch;
        }
        lastChar = ch;
    }

    public void reset() {
        lastChar = '\0';
        bufferLength = 0;
    }

    private void processSequence(final String sequence, final char terminator) {
        if (sequence.isEmpty()) {
            return;
        }
        // The OSC code is the prefix up to the first ';' (e.g. "4" in "4;16;rgb:..."). The rest
        // is the handler's payload. The code is parsed without the palette-index clamp (OSC codes
        // are unbounded — OSC 777/1337 must dispatch, not silently die at the 0-255 palette edge);
        // palette entries themselves are clamped inside the handlers via OSCParse.parseClampIndex.
        final int sep = sequence.indexOf(';');
        final int code = OSCParse.parseCode(sep < 0 ? sequence : sequence.substring(0, sep));
        if (code < 0) {
            return;
        }
        final OSCHandler handler = handlers.get(code);
        if (handler != null) {
            handler.execute(sep < 0 ? "" : sequence.substring(sep + 1), terminator);
        }
        // Unknown code (OSC 0/2/8/10/11 etc.) -> no-op until the follow-up bundle.
    }
}
