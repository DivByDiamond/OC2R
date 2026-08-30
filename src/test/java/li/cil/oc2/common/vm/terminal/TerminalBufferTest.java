package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import li.cil.oc2.common.vm.terminal.buffer.TerminalBuffer;
import li.cil.oc2.common.vm.terminal.color.TerminalColors;
import li.cil.oc2.common.vm.terminal.render.RendererModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Terminal smoke + VT100 parser integration tests.
 *
 * <p>{@link Terminal} keeps all client-only surface behind {@code @OnlyIn(Dist.CLIENT)}
 * ({@code getRenderer()}, {@code clientTick()}, ...) and creates the {@link TerminalClient}
 * lazily, so a plain {@code new Terminal()} works on the headless JUnit runtime classpath.
 * All sequences below are fed through the real {@link TerminalIO}/{@link TerminalOutput}
 * state machine (the same path the VM firmware uses) via {@code putOutput}.
 */
@SuppressWarnings("PMD.CyclomaticComplexity") // class-aggregate complexity is high because this is a growing
// suite of many small @Test methods — that's the point of a test class
public class TerminalBufferTest {
    private Terminal terminal;
    private TerminalBuffer buffer;
    private DummyRenderer renderer;
    // Shared test literals: keep these as constants so the many CSI sequences and sample
    // lines read as composable pieces (CSI = ESC + "[") and stay below PMD's
    // AvoidDuplicateLiterals threshold.
    private static final String ESC = "\u001b";
    private static final String CSI = ESC + "[";
    private static final String OSC = ESC + "]";
    private static final String BEL = "\u0007";
    private static final String ST = ESC + "\\";
    // A reusable OSC 4 set (entry 16 -> red, BEL-terminated) shared across the palette tests so
    // the rgb spec literal stays below PMD's AvoidDuplicateLiterals threshold.
    private static final String RED_RGB = "rgb:ff/00/00";
    private static final String OSC4_SET_16_RED = OSC + "4;16;" + RED_RGB + BEL;
    private static final String SAMPLE_LINE = "ABCDEFGH";
    private static final String MARGIN_CONTENT = "ABCDEFG";

    @BeforeEach
    void setUp() {
        terminal = new Terminal();
        buffer = terminal.bufferManager;
        renderer = new DummyRenderer();
        terminal.renderers.add(renderer);
    }

    @Test
    void initialBufferState() {
        assertNotNull(terminal);
        assertNotNull(buffer);
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
        assertEquals(24, terminal.lastRowToDisplay);
        assertEquals(24, terminal.lastRowToDisplayMax);
        assertEquals(Terminal.WIDTH * Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT, terminal.buffer.length);
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(Terminal.WIDTH - 1, Terminal.HEIGHT - 1));
        assertFalse(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertTrue(terminal.currentPrivateModeState.DECAWM);
        assertEquals(TerminalColors.ColorMode.DEFAULT_FOREGROUND, terminal.currentForegroundColorMode);
    }

    @Test
    void clearLineClearsRow() {
        write(terminal, SAMPLE_LINE + CSI + "2;1HXYZ");
        buffer.clearLine(0);
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals('X', charAt(0, 1));
    }

    @Test
    void shiftUpOneMovesRows() {
        write(terminal, CSI + "2;8r");
        fillRows(1, MARGIN_CONTENT);
        buffer.shiftUpOne();
        assertEquals(' ', charAt(0, 0));
        assertEquals('B', charAt(0, 1));
        assertEquals('G', charAt(0, 6));
        assertEquals(' ', charAt(0, 7));
    }

    @Test
    void shiftDownOneMovesRows() {
        write(terminal, CSI + "2;8r");
        fillRows(1, MARGIN_CONTENT);
        buffer.shiftDownOne();
        assertEquals(' ', charAt(0, 1));
        assertEquals('A', charAt(0, 2));
        assertEquals('F', charAt(0, 7));
        assertEquals(' ', charAt(0, 0));
    }

    @Test
    void clearAllLines() {
        write(terminal, SAMPLE_LINE + CSI + "2;1HXYZ");
        buffer.clear();
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(' ', charAt(0, 1));
    }

    @Test
    void cupMovesCursor() {
        write(terminal, CSI + "3;4H");
        assertEquals(3, terminal.x);
        assertEquals(2, terminal.y);
        write(terminal, CSI + "5;6f");
        assertEquals(5, terminal.x);
        assertEquals(4, terminal.y);
        write(terminal, CSI + "999;999H");
        assertEquals(Terminal.WIDTH - 1, terminal.x);
        assertEquals(Terminal.HEIGHT - 1, terminal.y);
        write(terminal, CSI + "H");
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
    }

    @Test
    void chaMovesColumnOnly() {
        write(terminal, CSI + "3;4H" + CSI + "40G");
        assertEquals(39, terminal.x);
        assertEquals(2, terminal.y);
        write(terminal, CSI + "G");
        assertEquals(0, terminal.x);
    }

    @Test
    void vpaMovesRowOnly() {
        write(terminal, CSI + "3;4H" + CSI + "7d");
        assertEquals(3, terminal.x);
        assertEquals(6, terminal.y);
    }

    @Test
    void dsrBareCsiNRepliesStatus() {
        // §36 m4: DSR (CSI Ps n) with no Ps defaults to Ps=5 (device status). CSIManager
        // substitutes the handler's defaultParameters before execute, so a bare CSI n
        // replies \033[0n instead of hanging a guest waiting for a status response.
        write(terminal, CSI + "n");
        final ByteBuffer reply = terminal.io.getInput();
        assertNotNull(reply, "bare CSI n must produce a status-report reply");
        final byte[] bytes = new byte[reply.remaining()];
        reply.get(bytes);
        assertEquals("\033[0n", new String(bytes, StandardCharsets.US_ASCII),
                "bare CSI n replies operating-status (\\033[0n)");
    }

    @Test
    void dsrExplicitFiveStillRepliesStatus() {
        // Explicit Ps=5 is unchanged by the defaultParameters fix — still a status report.
        write(terminal, CSI + "5n");
        final ByteBuffer reply = terminal.io.getInput();
        assertNotNull(reply, "CSI 5n must produce a status-report reply");
        final byte[] bytes = new byte[reply.remaining()];
        reply.get(bytes);
        assertEquals("\033[0n", new String(bytes, StandardCharsets.US_ASCII),
                "CSI 5n replies operating-status (\\033[0n)");
    }

    @Test
    void osc4SetChangesInstancePalette() {
        // OSC 4;Ps;Pt redefines palette entry Ps. Setting index 16 (first cube color) to red
        // must update the per-instance palette256, leaving the immutable default untouched.
        write(terminal, OSC4_SET_16_RED);
        assertEquals(0xff0000, terminal.palette256[16],
                "OSC 4 set must change the per-instance palette entry");
        assertNotEquals(TerminalColors.getDefaultPalette256()[16], terminal.palette256[16],
                "the changed entry must differ from the immutable default");
    }

    @Test
    void osc4SetStTerminatorWorks() {
        // ST-terminated (ESC then backslash) instead of BEL — exercises the two-byte
        // terminator: the ESC must arm lastChar without being buffered, then the backslash
        // byte completes ST (the broken-ST-detection bug site called out in the plan).
        write(terminal, OSC + "4;17;rgb:00/ff/00" + ST);
        assertEquals(0x00ff00, terminal.palette256[17],
                "OSC 4 set with the ST terminator must take effect");
    }

    @Test
    void osc4QueryRepliesRgb() {
        // OSC 4;Ps;? queries entry Ps. xterm replies 16-bit per channel (rgb:%04x) and reuses the
        // query's own terminator (misc.c:2647,2655): a BEL-terminated query gets a BEL-terminated
        // reply. Index 16 was set to 0xff0000 (red) -> r16=0xffff, g/b=0x0000.
        write(terminal, OSC4_SET_16_RED);
        write(terminal, OSC + "4;16;?" + BEL);
        final ByteBuffer reply = terminal.io.getInput();
        assertNotNull(reply, "OSC 4 query must produce a reply");
        final byte[] bytes = new byte[reply.remaining()];
        reply.get(bytes);
        assertEquals(ESC + "]4;16;rgb:ffff/0000/0000" + BEL,
                new String(bytes, StandardCharsets.US_ASCII),
                "OSC 4 query replies 16-bit per channel, mirroring the BEL terminator");
    }

    @Test
    void osc4QueryRepliesWithStTerminator() {
        // The same query ST-terminated must reply ST-terminated (ESC \), not BEL — the reply
        // mirrors the query's framing (F2: xterm unparseputc1(xw, final)).
        write(terminal, OSC4_SET_16_RED);
        write(terminal, OSC + "4;16;?" + ST);
        final ByteBuffer reply = terminal.io.getInput();
        assertNotNull(reply);
        final byte[] bytes = new byte[reply.remaining()];
        reply.get(bytes);
        assertEquals(ESC + "]4;16;rgb:ffff/0000/0000" + ST,
                new String(bytes, StandardCharsets.US_ASCII),
                "an ST-terminated query replies ST-terminated");
    }

    @Test
    void osc4BatchedSetsMultipleEntries() {
        // A single OSC 4 may carry several index;spec pairs (c;spec;c;spec;...), as xterm's
        // ChangeAnsiColorRequest (misc.c:2981) does. Both entries must be set, not just the first.
        write(terminal, OSC + "4;16;rgb:ff/00/00;17;rgb:00/ff/00" + BEL);
        assertEquals(0xff0000, terminal.palette256[16], "first pair (index 16) set");
        assertEquals(0x00ff00, terminal.palette256[17], "second pair (index 17) set");
    }

    @Test
    void osc4BatchedSetAndQueryInOneSequence() {
        // The batched loop handles mixed set+query pairs: a query pair replies, then a set pair
        // applies — both in one OSC 4.
        write(terminal, OSC + "4;16;?;17;rgb:00/ff/00" + BEL);
        assertEquals(0x00ff00, terminal.palette256[17], "the set pair applied");
        final ByteBuffer reply = terminal.io.getInput();
        assertNotNull(reply, "the query pair replied");
        final byte[] bytes = new byte[reply.remaining()];
        reply.get(bytes);
        // Index 16 is still default (0x000000) at this point -> reply reports default 16-bit.
        final int default16 = TerminalColors.getDefaultPalette256()[16];
        final int r16 = ((default16 >> 16) & 0xFF) * 0x0101;
        final int g16 = ((default16 >> 8) & 0xFF) * 0x0101;
        final int b16 = (default16 & 0xFF) * 0x0101;
        assertEquals(ESC + "]4;16;rgb:" + String.format("%04x/%04x/%04x", r16, g16, b16) + BEL,
                new String(bytes, StandardCharsets.US_ASCII),
                "the query pair replied with the (default) entry before the set pair applied");
    }

    @Test
    void osc4DoesNotMutateStaticDefault() {
        // SpotBugs MS_MUTABLE_ARRAY resolved by design: the static default is immutable. Setting
        // an entry on one terminal must not mutate the shared default. Revert-and-fail — a
        // shared static (palette256 aliased to COLORS_256) would change getDefaultPalette256()[1].
        final int defaultAt1 = TerminalColors.getDefaultPalette256()[1];
        write(terminal, OSC + "4;1;rgb:12/34/56" + BEL);
        assertEquals(defaultAt1, TerminalColors.getDefaultPalette256()[1],
                "OSC 4 set on an instance must not mutate the static default palette");
        assertEquals(0x123456, terminal.palette256[1],
                "the instance entry should reflect the OSC 4 set");
    }

    @Test
    void osc4PerTerminalIsolation() {
        // A real VT has a per-terminal palette: redefining a color on one terminal must not
        // bleed into another terminal's palette.
        final Terminal other = new Terminal();
        final int default16 = TerminalColors.getDefaultPalette256()[16];
        write(terminal, OSC4_SET_16_RED);
        assertEquals(0xff0000, terminal.palette256[16], "the sender's entry is redefined");
        assertEquals(default16, other.palette256[16],
                "the other terminal's entry must stay at the default");
    }

    @Test
    void osc104ResetsIndex() {
        // OSC 104;Ps resets entry Ps to its default (set/reset, no reply).
        write(terminal, OSC4_SET_16_RED);
        assertEquals(0xff0000, terminal.palette256[16], "precondition: entry was redefined");
        write(terminal, OSC + "104;16" + BEL);
        assertEquals(TerminalColors.getDefaultPalette256()[16], terminal.palette256[16],
                "OSC 104;Ps resets that entry to the default");
    }

    @Test
    void osc104ResetsAll() {
        // OSC 104 with no Ps resets the whole palette to defaults.
        final int[] defaults = TerminalColors.getDefaultPalette256();
        write(terminal, OSC4_SET_16_RED);
        write(terminal, OSC + "4;200;rgb:aa/bb/cc" + BEL);
        assertNotEquals(defaults[16], terminal.palette256[16], "precondition: entries were redefined");
        write(terminal, OSC + "104" + BEL);
        assertArrayEquals(defaults, terminal.palette256,
                "OSC 104 with no Ps resets the whole palette to defaults");
    }

    @Test
    void cudMovesCursorDownAndClampsSaturatedCount() {
        // parseArgument saturates at Integer.MAX_VALUE; terminal.y + args[0] must not overflow to a
        // negative int (which would clamp to row 0) — a huge down-move lands on the bottom row.
        write(terminal, CSI + "6;1H");          // row 6 (y=5)
        write(terminal, CSI + "3B");            // down 3 -> y=8
        assertEquals(0, terminal.x);
        assertEquals(8, terminal.y);
        write(terminal, CSI + "2147483647B");   // saturated down -> bottom
        assertEquals(Terminal.HEIGHT - 1, terminal.y, "saturated CUD lands on the bottom row, not 0");
        assertEquals(0, terminal.x);
    }

    @Test
    void cufMovesCursorRightAndClampsSaturatedCount() {
        // Mirror of CUD: terminal.x + args[0] must not overflow to column 0; a huge right-move
        // lands on the right edge.
        write(terminal, CSI + "1;6H");          // col 6 (x=5)
        write(terminal, CSI + "3C");            // right 3 -> x=8
        assertEquals(8, terminal.x);
        assertEquals(0, terminal.y);
        write(terminal, CSI + "2147483647C");   // saturated right -> right edge
        assertEquals(Terminal.WIDTH - 1, terminal.x, "saturated CUF lands on the right edge, not 0");
        assertEquals(0, terminal.y);
    }

    @Test
    void cuuMovesCursorUpAndClampsSaturatedCount() {
        // CUU subtracts (no overflow), but shares the bounded moveCursorBy primitive; a huge
        // up-move lands on the top row.
        write(terminal, CSI + "6;1H");          // row 6 (y=5)
        write(terminal, CSI + "3A");            // up 3 -> y=2
        assertEquals(0, terminal.x);
        assertEquals(2, terminal.y);
        write(terminal, CSI + "2147483647A");   // saturated up -> top
        assertEquals(0, terminal.y, "saturated CUU lands on the top row");
        assertEquals(0, terminal.x);
    }

    @Test
    void cubMovesCursorLeftAndClampsSaturatedCount() {
        // CUB subtracts (no overflow), but shares the bounded moveCursorBy primitive; a huge
        // left-move lands on column 0.
        write(terminal, CSI + "1;6H");          // col 6 (x=5)
        write(terminal, CSI + "3D");            // left 3 -> x=2
        assertEquals(2, terminal.x);
        assertEquals(0, terminal.y);
        write(terminal, CSI + "2147483647D");   // saturated left -> col 0
        assertEquals(0, terminal.x, "saturated CUB lands on column 0");
        assertEquals(0, terminal.y);
    }

    @Test
    void vprMovesCursorDownAndClampsSaturatedCount() {
        // VPR (CSI Ps e) is the relative mirror of VPA; same overflow class as CUD.
        write(terminal, CSI + "6;1H");          // row 6 (y=5)
        write(terminal, CSI + "3e");            // down 3 -> y=8
        assertEquals(0, terminal.x);
        assertEquals(8, terminal.y);
        write(terminal, CSI + "2147483647e");   // saturated -> bottom
        assertEquals(Terminal.HEIGHT - 1, terminal.y, "saturated VPR lands on the bottom row, not 0");
        assertEquals(0, terminal.x);
    }

    @Test
    void hprMovesCursorRightAndClampsSaturatedCount() {
        // HPR (CSI Ps a) is the relative mirror of HPA; same overflow class as CUF.
        write(terminal, CSI + "1;6H");          // col 6 (x=5)
        write(terminal, CSI + "3a");            // right 3 -> x=8
        assertEquals(8, terminal.x);
        assertEquals(0, terminal.y);
        write(terminal, CSI + "2147483647a");   // saturated -> right edge
        assertEquals(Terminal.WIDTH - 1, terminal.x, "saturated HPR lands on the right edge, not 0");
        assertEquals(0, terminal.y);
    }

    @Test
    void cnlMovesToNextLineAndClampsSaturatedCount() {
        // CNL (CSI Ps E) moves down Ps lines and to column 0. Same overflow class as CUD on the
        // row; the column reset is why it can't share moveCursorBy (which preserves the column).
        write(terminal, CSI + "1;6H");          // col 6 (x=5), row 1 (y=0)
        write(terminal, CSI + "3E");            // down 3, col 0 -> y=3, x=0
        assertEquals(0, terminal.x);
        assertEquals(3, terminal.y);
        write(terminal, CSI + "2147483647E");   // saturated -> bottom, col 0
        assertEquals(0, terminal.x);
        assertEquals(Terminal.HEIGHT - 1, terminal.y, "saturated CNL lands on the bottom row, not 0");
    }

    @Test
    void moveCursorBySaturatedDownInScrollRegionClampsToScrollLast() {
        // moveCursorBy bounds the delta to +/- HEIGHT before the add, then setClampedCursorPos
        // applies the scroll-region clamp. From inside a region [5..10] (0-indexed 4..9) at y=6,
        // a saturated CUD (down) must land on scrollLast (9) — not overflow the int sum to a
        // negative value that clamps to scrollFirst (4), and not escape the region to HEIGHT-1.
        write(terminal, CSI + "5;10r");     // scroll region rows 5-10 (0-indexed 4..9)
        assertEquals(4, terminal.scrollFirst);
        assertEquals(9, terminal.scrollLast);
        write(terminal, CSI + "7;1H");      // row 7 (y=6), inside the region
        assertEquals(6, terminal.y);
        write(terminal, CSI + "2147483647B"); // saturated down
        assertEquals(9, terminal.y, "saturated CUD inside a scroll region lands on scrollLast, not scrollFirst or HEIGHT-1");
        assertEquals(0, terminal.x);
    }

    @Test
    void edClearsFromCursorToEndOfScreen() {
        write(terminal, SAMPLE_LINE + CSI + "3G" + CSI + "J");
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        assertEquals(' ', charAt(2, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(2, terminal.x);
        assertEquals(0, terminal.y);
    }

    @Test
    void edClearsFromStartOfScreenToCursor() {
        write(terminal, SAMPLE_LINE + CSI + "5G" + CSI + "1J");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(4, 0));
        assertEquals('F', charAt(5, 0));
        assertEquals('H', charAt(7, 0));
    }

    @Test
    void edClearsWholeScreenWithoutMovingCursor() {
        write(terminal, SAMPLE_LINE + CSI + "6;11H" + CSI + "2J");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(' ', charAt(0, 5));
        assertEquals(10, terminal.x);
        assertEquals(5, terminal.y);
    }

    @Test
    void elClearsFromCursorToEndOfLine() {
        write(terminal, SAMPLE_LINE + CSI + "5G" + CSI + "K");
        assertEquals('A', charAt(0, 0));
        assertEquals('D', charAt(3, 0));
        assertEquals(' ', charAt(4, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(4, terminal.x);
    }

    @Test
    void elClearsFromStartOfLineToCursor() {
        write(terminal, SAMPLE_LINE + CSI + "5G" + CSI + "1K");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(4, 0));
        assertEquals('F', charAt(5, 0));
        assertEquals('H', charAt(7, 0));
    }

    @Test
    void elClearsWholeLine() {
        write(terminal, SAMPLE_LINE + CSI + "2K");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(8, terminal.x);
    }

    @Test
    void decstbmSetsMarginsAndHomesCursor() {
        write(terminal, CSI + "3;8r");
        assertEquals(2, terminal.scrollFirst);
        assertEquals(7, terminal.scrollLast);
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
        write(terminal, CSI + "2;3r");
        assertEquals(1, terminal.scrollFirst);
        assertEquals(2, terminal.scrollLast);
    }

    @Test
    void decstbmIgnoresDegenerateMargins() {
        write(terminal, CSI + "5;5r");
        assertEquals(0, terminal.scrollFirst);
        assertEquals(Terminal.HEIGHT - 1, terminal.scrollLast);
        write(terminal, CSI + "10;3r");
        assertEquals(0, terminal.scrollFirst);
        assertEquals(Terminal.HEIGHT - 1, terminal.scrollLast);
    }

    @Test
    void decomOriginModeClampsCursorToMargins() {
        write(terminal, CSI + "3;8r" + CSI + "?6h");
        assertTrue(terminal.currentPrivateModeState.DECOM);
        assertEquals(0, terminal.x);
        assertEquals(2, terminal.y);
        write(terminal, CSI + "4;4H");
        assertEquals(3, terminal.x);
        assertEquals(5, terminal.y);
        write(terminal, CSI + "5d");
        assertEquals(3, terminal.x);
        assertEquals(6, terminal.y);
        write(terminal, CSI + "?6l");
        assertFalse(terminal.currentPrivateModeState.DECOM);
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
    }

    @Test
    void decomSaturatedRowClampsToScrollLastNotOverflow() {
        // Under DECOM, CUP's row is origin-relative: absolute = scrollFirst + (row-1). With a
        // saturated row, scrollFirst + (MAX_VALUE-1) overflows negative and clamps to scrollFirst
        // (top) instead of scrollLast (bottom). The fix bounds the row to the region first.
        write(terminal, CSI + "3;8r" + CSI + "?6h");  // scroll region rows 3-8 (0-indexed 2..7), DECOM on
        assertEquals(2, terminal.scrollFirst);
        assertEquals(7, terminal.scrollLast);
        write(terminal, CSI + "2147483647;1H");       // CUP row MAX, col 1 -> bottom of region
        assertEquals(7, terminal.y, "saturated DECOM row lands on scrollLast (bottom), not scrollFirst");
        assertEquals(0, terminal.x);
    }

    @Test
    void insertLinesShiftsContentDownWithinMargins() {
        write(terminal, CSI + "2;8r");
        fillRows(1, MARGIN_CONTENT);
        write(terminal, CSI + "4;1H" + CSI + "2L");
        assertEquals('A', charAt(0, 1));
        assertEquals('B', charAt(0, 2));
        assertEquals(' ', charAt(0, 3));
        assertEquals(' ', charAt(0, 4));
        assertEquals('C', charAt(0, 5));
        assertEquals('D', charAt(0, 6));
        assertEquals('E', charAt(0, 7));
        assertEquals(' ', charAt(0, 0));
    }

    @Test
    void deleteLinesShiftsContentUpWithinMargins() {
        write(terminal, CSI + "2;8r");
        fillRows(1, MARGIN_CONTENT);
        write(terminal, CSI + "4;1H" + CSI + "2M");
        assertEquals('A', charAt(0, 1));
        assertEquals('B', charAt(0, 2));
        assertEquals('E', charAt(0, 3));
        assertEquals('F', charAt(0, 4));
        assertEquals('G', charAt(0, 5));
        assertEquals(' ', charAt(0, 6));
        assertEquals(' ', charAt(0, 7));
        assertEquals(' ', charAt(0, 0));
    }

    @Test
    void scrollUpMovesContentUpWithinMargins() {
        write(terminal, CSI + "2;8r");
        fillRows(1, MARGIN_CONTENT);
        write(terminal, CSI + "2S");
        assertEquals(' ', charAt(0, 0));
        assertEquals('C', charAt(0, 1));
        assertEquals('D', charAt(0, 2));
        assertEquals('G', charAt(0, 5));
        assertEquals(' ', charAt(0, 6));
        assertEquals(' ', charAt(0, 7));
    }

    @Test
    void scrollDownMovesContentDownWithinMargins() {
        write(terminal, CSI + "2;8r");
        fillRows(1, MARGIN_CONTENT);
        write(terminal, CSI + "2T");
        assertEquals(' ', charAt(0, 1));
        assertEquals(' ', charAt(0, 2));
        assertEquals('A', charAt(0, 3));
        assertEquals('B', charAt(0, 4));
        assertEquals('C', charAt(0, 5));
        assertEquals('D', charAt(0, 6));
        assertEquals('E', charAt(0, 7));
        assertEquals(' ', charAt(0, 0));
    }

    @Test
    void scrollUpWithScrollbackGrowsViewWindow() {
        fillRows(0, "ABCD");
        write(terminal, CSI + "1S");
        assertEquals('B', charAt(0, 0));
        assertEquals('C', charAt(0, 1));
        assertEquals('D', charAt(0, 2));
        assertEquals(' ', charAt(0, 3));
        assertEquals(25, terminal.lastRowToDisplayMax);
    }

    @Test
    void scrollUpCountMovesMultipleLines() {
        fillRows(0, "ABCD");
        write(terminal, CSI + "2S");
        assertEquals('C', charAt(0, 0));
        assertEquals('D', charAt(0, 1));
        assertEquals(' ', charAt(0, 2));
        assertEquals(26, terminal.lastRowToDisplayMax);
    }

    @Test
    void scrollDownCountMovesMultipleLines() {
        fillRows(1, "ABCD");
        write(terminal, CSI + "2T");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(0, 1));
        assertEquals(' ', charAt(0, 2));
        assertEquals('A', charAt(0, 3));
        assertEquals('B', charAt(0, 4));
        assertEquals('C', charAt(0, 5));
        assertEquals('D', charAt(0, 6));
    }

    @Test
    void altBuffer47SwitchesAndPreservesMain() {
        write(terminal, "Hello");
        write(terminal, CSI + "?47h");
        assertTrue(terminal.currentPrivateModeState.isAltBufferEnabled());
        write(terminal, "World");
        assertEquals('H', charAt(0, 0));
        assertEquals('W', altCharAt(0, 0));
        assertEquals(' ', altCharAt(5, 0));
        write(terminal, CSI + "?47l");
        assertFalse(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals('H', charAt(0, 0));
    }

    @Test
    void altBuffer1047Switches() {
        write(terminal, "Hello");
        write(terminal, CSI + "?1047h");
        assertTrue(terminal.currentPrivateModeState.isAltBufferEnabled());
        write(terminal, "abc");
        assertEquals('H', charAt(0, 0));
        assertEquals('a', altCharAt(0, 0));
        write(terminal, CSI + "?1047l");
        assertFalse(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals('H', charAt(0, 0));
    }

    @Test
    void altBuffer1049SavesAndRestoresCursor() {
        write(terminal, "A" + CSI + "5;6H");
        write(terminal, CSI + "?1049h");
        assertTrue(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
        write(terminal, "B" + CSI + "10;10H");
        assertEquals(9, terminal.x);
        assertEquals(9, terminal.y);
        write(terminal, CSI + "?1049l");
        assertFalse(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals(5, terminal.x);
        assertEquals(4, terminal.y);
        assertEquals('A', charAt(0, 0));
    }

    @Test
    void altBufferMarksScreenDirtyOnSwitch() {
        resetDirty();
        write(terminal, CSI + "?47h");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
        resetDirty();
        write(terminal, CSI + "?47l");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
        resetDirty();
        write(terminal, CSI + "?1049h");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
    }

    @Test
    void pendingWrapWrapsToNextLine() {
        write(terminal, "A".repeat(Terminal.WIDTH) + "B");
        assertEquals('A', charAt(0, 0));
        assertEquals('A', charAt(Terminal.WIDTH - 1, 0));
        assertEquals('B', charAt(0, 1));
        assertEquals(1, terminal.x);
        assertEquals(1, terminal.y);
    }

    @Test
    void pendingWrapDisabledOverwritesLastColumn() {
        write(terminal, CSI + "?7l");
        write(terminal, "A".repeat(Terminal.WIDTH) + "B");
        assertEquals('B', charAt(Terminal.WIDTH - 1, 0));
        assertEquals(' ', charAt(0, 1));
        assertEquals(0, terminal.y);
    }

    @Test
    void backspaceFromAutowrapPendingMovesToWidthMinusTwo() {
        // vttest suite 1, "autowrap, mixing control and print characters" case 1:
        // fill a row to the right margin (cursor enters autowrap-pending), then BS.
        // BS must clear pending and move to column width-2 so the next printable
        // lands one column left of the margin and the just-written margin char
        // survives; the cursor must not rest at a phantom column width either.
        // Regressed in 1b978ad, which dropped the pending-aware clamp and made BS
        // land back on width-1, letting the next printable overwrite the margin.
        write(terminal, "A".repeat(Terminal.WIDTH)); // fill row 0 to the right margin
        write(terminal, "\b");                       // BS from autowrap-pending
        write(terminal, "B");
        assertEquals('A', charAt(Terminal.WIDTH - 1, 0),
            "last-column 'A' must survive BS from autowrap-pending");
        assertEquals('B', charAt(Terminal.WIDTH - 2, 0),
            "post-BS printable must land at column width-2, not overwrite the margin");
        assertEquals(Terminal.WIDTH - 1, terminal.x,
            "cursor must rest at width-1 after the non-margin write, not a phantom width");
    }

    @Test
    void autowrapPendingFlagLifecycle() {
        // Directly tests the autowrapPending flag's set/clear transitions rather than
        // inferring them from cell contents (the companion test above does the latter).
        // This is the state vttest's DECCIR report exposes as the aw_pending bit; the
        // flag is queried directly here instead of via a terminal-state-report escape.

        // (1) Writing the last column arms pending; the cursor stays at width-1.
        write(terminal, "A".repeat(Terminal.WIDTH));
        assertTrue(terminal.autowrapPending, "filling the last column must arm autowrap-pending");
        assertEquals(Terminal.WIDTH - 1, terminal.x,
            "cursor must stay at width-1 while pending, not advance to a phantom width");

        // (2) BS clears pending (BS is a cursor move).
        write(terminal, "\b");
        assertFalse(terminal.autowrapPending, "BS must clear autowrap-pending");

        // (3) Re-arming pending, then the next printable fires the wrap and clears pending.
        write(terminal, CSI + "1;1H" + "A".repeat(Terminal.WIDTH)); // home + re-fill row 0
        assertTrue(terminal.autowrapPending, "re-filling the last column re-arms pending");
        write(terminal, "B");                                       // fires the deferred wrap
        assertFalse(terminal.autowrapPending, "the wrap fired by the next printable must clear pending");
        assertEquals(1, terminal.x, "after the wrap the cursor sits at column 1 (post-write on row 1)");

        // (4) With DECAWM off, filling the last column must NOT arm pending (overwrite, not wrap).
        write(terminal, CSI + "?7l");
        write(terminal, CSI + "1;1H" + "A".repeat(Terminal.WIDTH));
        assertFalse(terminal.autowrapPending,
            "filling the last column with DECAWM off must not arm pending");
    }

    @Test
    void dirtyMaskMarksScreenOnScroll() {
        resetDirty();
        write(terminal, CSI + "2S");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
    }

    @Test
    void dirtyMaskMarksMarginRowsOnScroll() {
        write(terminal, CSI + "2;8r");
        fillRows(1, MARGIN_CONTENT);
        resetDirty();
        write(terminal, CSI + "2S");
        assertEquals(0b11111110, renderer.dirtyMask.get() & 0xFF);
    }

    @Test
    void dirtyMaskMarksClearedLine() {
        write(terminal, "X" + CSI + "5;1H");
        resetDirty();
        write(terminal, CSI + "K");
        assertEquals(1 << 4, renderer.dirtyMask.get());
    }

    @Test
    void dirtyMaskMarksWrittenRow() {
        resetDirty();
        write(terminal, "X");
        assertEquals(1, renderer.dirtyMask.get());
    }

    // --- setClampedCursorPos: cursor outside scroll region should NOT clamp Y ---

    @Test
    void setClampedCursorPosOutsideScrollRegionDoesNotClampY() {
        // Set a scroll region [5..10] (0-indexed: rows 4..9)
        write(terminal, CSI + "5;10r");
        assertEquals(4, terminal.scrollFirst);
        assertEquals(9, terminal.scrollLast);
        // Move cursor to row 1 (0-indexed: 0), which is outside the scroll region
        write(terminal, CSI + "1;1H");
        assertEquals(0, terminal.y);
        // setClampedCursorPos with a Y outside the scroll region should NOT clamp
        // because cursor is already outside the scroll region
        terminal.setClampedCursorPos(10, 20);
        assertEquals(10, terminal.x);
        assertEquals(20, terminal.y);
    }

    @Test
    void setClampedCursorPosInsideScrollRegionClampsY() {
        // Set a scroll region [5..10] (0-indexed: rows 4..9)
        write(terminal, CSI + "5;10r");
        // Move cursor into the scroll region
        write(terminal, CSI + "7;1H");
        assertEquals(6, terminal.y);
        assertTrue(terminal.y >= terminal.scrollFirst && terminal.y <= terminal.scrollLast);
        // setClampedCursorPos should clamp Y to scroll region [4..9]
        // Request Y=20 (well beyond scrollLast=9)
        terminal.setClampedCursorPos(10, 20);
        assertEquals(10, terminal.x);
        assertEquals(9, terminal.y); // clamped to scrollLast
        // Request Y=0 (well before scrollFirst=4)
        terminal.setClampedCursorPos(10, 0);
        assertEquals(10, terminal.x);
        assertEquals(4, terminal.y); // clamped to scrollFirst
        // Request Y within scroll region — should stay as-is
        terminal.setClampedCursorPos(10, 7);
        assertEquals(10, terminal.x);
        assertEquals(7, terminal.y);
    }

    // --- RIS resets savePrivateModeState to defaults ---

    @Test
    void risResetsSavePrivateModeState() {
        // Modify private modes, then XTSAVE to capture them into savePrivateModeState
        write(terminal, CSI + "?7l");        // DECAWM off
        write(terminal, CSI + "?6h");        // DECOM on
        write(terminal, CSI + "?7s");        // XTSAVE mode 7 (DECAWM)
        write(terminal, CSI + "?6s");        // XTSAVE mode 6 (DECOM)
        // Verify savePrivateModeState captured the modified values
        assertFalse(terminal.savePrivateModeState.DECAWM, "DECAWM should be saved as off");
        assertTrue(terminal.savePrivateModeState.DECOM, "DECOM should be saved as on");
        // Now RIS
        write(terminal, ESC + "c");
        // savePrivateModeState should be reset to defaults
        assertTrue(terminal.savePrivateModeState.DECAWM, "DECAWM should be default (on) after RIS");
        assertFalse(terminal.savePrivateModeState.DECOM, "DECOM should be default (off) after RIS");
    }

    // --- RIS resets terminal.state to NORMAL ---

    @Test
    void risResetsTerminalStateToNormal() {
        // Put the terminal into an escape state by sending ESC followed by a non-completing char
        write(terminal, CSI + "3");
        // We should be in CONTROL_SEQUENCE state (partial CSI)
        assertEquals(Terminal.State.CONTROL_SEQUENCE, terminal.state);
        // Now RIS
        write(terminal, ESC + "c");
        assertEquals(Terminal.State.NORMAL, terminal.state);
    }

    @Test
    void risResetsStateFromEscape() {
        // Put terminal into ESCAPE state
        write(terminal, ESC);
        assertEquals(Terminal.State.ESCAPE, terminal.state);
        // RIS
        write(terminal, ESC + "c");
        assertEquals(Terminal.State.NORMAL, terminal.state);
    }

    // --- RIS clears terminal.input ---

    @Test
    void risClearsInput() {
        // Enqueue some input bytes via the TerminalIO API
        terminal.io.putInput((byte) 'A');
        terminal.io.putInput((byte) 'B');
        terminal.io.putInput((byte) 'C');
        // Verify input is non-empty (readInput returns -1 when empty)
        assertNotEquals(-1, terminal.io.readInput(), "input should be non-empty before RIS");
        // RIS
        write(terminal, ESC + "c");
        assertEquals(-1, terminal.io.readInput(), "input queue should be empty after RIS");
    }

    // --- getDirtyRow refactor: scrolling main buffer with scrollback marks correct dirty lines ---

    @Test
    void dirtyMaskScrollMainBufferWithScrollback() {
        // Fill rows 0-3 with distinct content (the "nano bug" scenario)
        fillRows(0, "ABCD");
        // Scroll up 1 line — this grows lastRowToDisplayMax to 25
        resetDirty();
        write(terminal, CSI + "1S");
        assertEquals(25, terminal.lastRowToDisplayMax);
        // After scrolling up 1, rows 0-2 have B,C,D and row 3 is blank.
        // All 24 visible rows should be marked dirty (content shifted up by 1).
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
    }

    @Test
    void dirtyMaskScrollMainBufferWithScrollbackTwoLines() {
        // Fill rows 0-3 with content
        fillRows(0, "ABCD");
        // Scroll up 2 lines — lastRowToDisplayMax grows to 26
        resetDirty();
        write(terminal, CSI + "2S");
        assertEquals(26, terminal.lastRowToDisplayMax);
        // All visible rows dirty
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
    }

    @Test
    void dirtyMaskScrollMainBufferWithScrollbackMatchesWrittenRows() {
        // This test verifies the getDirtyRow refactor: after scrolling with scrollback,
        // the dirty mask should match exactly the rows that actually changed.
        // We write to a specific row after scroll and verify the dirty bit goes to the right place.
        fillRows(0, "AB");
        write(terminal, CSI + "1S"); // scroll up 1, lastRowToDisplayMax=25
        resetDirty();
        // Write a character at row 0 — should mark row 0 dirty
        write(terminal, CSI + "1;1HZ");
        assertEquals(1, renderer.dirtyMask.get(), "Only row 0 should be dirty after writing to row 0");
        assertEquals('Z', charAt(0, 0));
    }

    @Test
    void dirtyMaskScrollUpInMarginRegionWithScrollback() {
        // Set scroll region [2..8] (0-indexed: 1..7)
        write(terminal, CSI + "2;8r");
        fillRows(1, MARGIN_CONTENT);
        // Scroll up 1 within the margin
        resetDirty();
        write(terminal, CSI + "1S");
        // Rows 1..7 should be dirty (the scroll region), row 0 should NOT
        int expected = 0;
        for (int i = 1; i <= 7; i++) {
            expected |= (1 << i);
        }
        assertEquals(expected, renderer.dirtyMask.get() & 0xFF);
    }

    // --- Screen features: ECH / DCH / ICH / IRM / DECSCNM ---

    @Test
    void echErasesCharsFromCursorWithoutShifting() {
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");     // cursor to col 3 (x=2, the 'C')
        write(terminal, CSI + "2X");     // ECH 2: erase 2 chars, no shift
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        assertEquals(' ', charAt(2, 0), "ECH blanks the char at the cursor");
        assertEquals(' ', charAt(3, 0), "ECH blanks N chars from the cursor");
        assertEquals('E', charAt(4, 0), "ECH must not shift; later chars stay put");
        assertEquals('F', charAt(5, 0));
    }

    @Test
    void dchDeletesCharsShiftingLeft() {
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");     // x=2 (C)
        write(terminal, CSI + "2P");     // DCH 2: delete 2, shift left, blank the tail
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        assertEquals('E', charAt(2, 0), "DCH shifts chars left into the deleted gap");
        assertEquals('F', charAt(3, 0));
        assertEquals('G', charAt(4, 0));
        assertEquals('H', charAt(5, 0));
        assertEquals(' ', charAt(6, 0), "DCH fills the tail with blanks");
        assertEquals(' ', charAt(7, 0));
    }

    @Test
    void ichInsertsBlanksShiftingRight() {
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");     // x=2 (C)
        write(terminal, CSI + "2@");     // ICH 2: insert 2 blanks, shift right
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        assertEquals(' ', charAt(2, 0), "ICH inserts blanks at the cursor");
        assertEquals(' ', charAt(3, 0));
        assertEquals('C', charAt(4, 0), "ICH shifts existing chars right");
        assertEquals('D', charAt(5, 0));
        assertEquals('E', charAt(6, 0));
        assertEquals('F', charAt(7, 0));
    }

    @Test
    void irmInsertsCharsShiftingRight() {
        write(terminal, "AB");
        write(terminal, CSI + "1G");     // x=0 (A)
        write(terminal, CSI + "4h");     // IRM on (SM 4)
        write(terminal, "X");             // insert X at col 0; A,B shift right
        assertEquals('X', charAt(0, 0));
        assertEquals('A', charAt(1, 0), "IRM shifts existing chars right");
        assertEquals('B', charAt(2, 0));
        write(terminal, CSI + "4l");     // IRM off (RM 4)
        write(terminal, CSI + "1G");     // x=0
        write(terminal, "Y");             // overwrite in place, no shift
        assertEquals('Y', charAt(0, 0));
        assertEquals('A', charAt(1, 0), "with IRM off, writes overwrite in place");
    }

    // --- Char ops while scrolled back must mark the rendered screen row (getDirtyRow) ---

    @Test
    void echWhileScrolledBackMarksCorrectScreenRow() {
        // Grow scrollback so lastRowToDisplayMax exceeds HEIGHT; the view stays at the bottom
        // (lastRowToDisplay == lastRowToDisplayMax), which alone does not expose the bug.
        for (int i = 0; i < 30; i++) write(terminal, "\n");
        // Scroll the view one line back into scrollback. The cursor's bottom-window row now
        // renders (lastRowToDisplayMax - lastRowToDisplay) screen rows below terminal.y.
        buffer.decrementLastLineToDisplay();
        final int scrollBack = terminal.lastRowToDisplayMax - terminal.lastRowToDisplay;
        assertTrue(scrollBack >= 1, "view should be scrolled back into scrollback");
        // Cursor to the top row; its buffer row is still visible and renders at row scrollBack.
        write(terminal, CSI + "1;1H");
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "1;1H");
        resetDirty();
        write(terminal, CSI + "2X"); // ECH 2: erase 2 chars at the cursor, no shift
        // The erased buffer row renders at screen row scrollBack, not at row 0 (terminal.y).
        assertEquals(1 << scrollBack, renderer.dirtyMask.get() & 0xFFFFFF,
            "ECH while scrolled back must mark the screen row where the cursor row renders, not terminal.y");
    }

    @Test
    void dchWhileScrolledBackMarksCorrectScreenRow() {
        for (int i = 0; i < 30; i++) write(terminal, "\n");
        buffer.decrementLastLineToDisplay();
        final int scrollBack = terminal.lastRowToDisplayMax - terminal.lastRowToDisplay;
        assertTrue(scrollBack >= 1, "view should be scrolled back into scrollback");
        write(terminal, CSI + "1;1H");
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "1;1H");
        resetDirty();
        write(terminal, CSI + "2P"); // DCH 2: delete 2, shift left, blank the tail
        assertEquals(1 << scrollBack, renderer.dirtyMask.get() & 0xFFFFFF,
            "DCH while scrolled back must mark the screen row where the cursor row renders, not terminal.y");
    }

    @Test
    void ichWhileScrolledBackMarksCorrectScreenRow() {
        for (int i = 0; i < 30; i++) write(terminal, "\n");
        buffer.decrementLastLineToDisplay();
        final int scrollBack = terminal.lastRowToDisplayMax - terminal.lastRowToDisplay;
        assertTrue(scrollBack >= 1, "view should be scrolled back into scrollback");
        write(terminal, CSI + "1;1H");
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "1;1H");
        resetDirty();
        write(terminal, CSI + "2@"); // ICH 2: insert 2 blanks, shift right
        assertEquals(1 << scrollBack, renderer.dirtyMask.get() & 0xFFFFFF,
            "ICH while scrolled back must mark the screen row where the cursor row renders, not terminal.y");
    }

    // --- DCH/ICH arg-0 (normalized to 1 by the dispatcher) and arg-overflow (clamped to width) ---

    @Test
    void dchArgZeroDeletesOneChar() {
        // CSIManager replaces arg 0 with the default (1) before the handler runs, so an
        // explicit 0 deletes exactly one character rather than being a no-op.
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");    // x=2 (C)
        write(terminal, CSI + "0P");     // DCH 0 -> delete 1
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        assertEquals('D', charAt(2, 0), "arg 0 is normalized to 1, deleting one char");
        assertEquals('E', charAt(3, 0));
        assertEquals('H', charAt(6, 0));
        assertEquals(' ', charAt(7, 0), "the freed tail cell is blanked");
    }

    @Test
    void ichArgZeroInsertsOne() {
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");    // x=2 (C)
        write(terminal, CSI + "0@");     // ICH 0 -> insert 1
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        assertEquals(' ', charAt(2, 0), "arg 0 is normalized to 1, inserting one blank");
        assertEquals('C', charAt(3, 0), "existing chars shift right by one");
        assertEquals('D', charAt(4, 0));
    }

    @Test
    void dchArgOverflowClearsToEndOfLine() {
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");    // x=2 (C)
        write(terminal, CSI + "999P");   // DCH 999 -> clamp to width, clear to end of line
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        for (int x = 2; x < Terminal.WIDTH; x++) {
            assertEquals(' ', charAt(x, 0), "overflow count clears from the cursor to end of line");
        }
    }

    @Test
    void ichArgOverflowBlanksToEndOfLine() {
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");    // x=2 (C)
        write(terminal, CSI + "999@");   // ICH 999 -> clamp to width, blank to end of line
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        for (int x = 2; x < Terminal.WIDTH; x++) {
            assertEquals(' ', charAt(x, 0), "overflow count blanks from the cursor to end of line");
        }
    }

    @Test
    void decscnmToggleMarksWholeScreenDirty() {
        assertFalse(terminal.currentPrivateModeState.DECSCNM);
        resetDirty();
        write(terminal, CSI + "?5h");    // DECSCNM on
        assertTrue(terminal.currentPrivateModeState.DECSCNM, "?5h enables screen-inverse");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF, "DECSCNM toggle must redraw the whole screen");
        resetDirty();
        write(terminal, CSI + "?5l");    // DECSCNM off
        assertFalse(terminal.currentPrivateModeState.DECSCNM);
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF, "DECSCNM toggle must redraw the whole screen");
    }

    @Test
    void xtrestoreDecscnmRestoresAndMarksWholeScreenDirty() {
        write(terminal, CSI + "?5h");    // DECSCNM on
        write(terminal, CSI + "?5s");    // XTSAVE mode 5: save DECSCNM=true
        assertTrue(terminal.savePrivateModeState.DECSCNM, "XTSAVE must capture DECSCNM");
        write(terminal, CSI + "?5l");    // DECSCNM off
        assertFalse(terminal.currentPrivateModeState.DECSCNM);
        resetDirty();
        write(terminal, CSI + "?5r");    // XTRESTORE mode 5: restore DECSCNM=true
        assertTrue(terminal.currentPrivateModeState.DECSCNM, "XTRESTORE must restore DECSCNM");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF,
            "restoring DECSCNM via XTRESTORE must redraw the whole screen, like DECSET/DECRST");
    }

    @Test
    void echErasedCellsTakeDefaultForegroundAndCurrentBackground() {
        write(terminal, CSI + "41m");    // bg = SIXTEEN_COLOR red (sixteenColor.g = 1)
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");     // x=2
        write(terminal, CSI + "2X");     // ECH 2
        final int idx = cellIndex(2, 0);
        assertEquals(TerminalColors.ColorMode.DEFAULT_FOREGROUND, terminal.colors[idx].mode,
            "erased cell foreground must be the DEFAULT_FOREGROUND marker");
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.colorsBackground[idx].mode,
            "erased cell background must keep the current bg mode");
        assertEquals(1, terminal.colorsBackground[idx].g,
            "erased cell background must keep the current bg color (red)");
        assertEquals(TerminalColors.DEFAULT_STYLE, terminal.styles[idx],
            "erased cell style must reset to default");
    }

    @Test
    void xtRawPassthroughRendersEscapesAsGlyphsNotInterpreted() {
        // CSI ?7777h turns on raw passthrough (the built-in byte-capture debugger): every byte
        // is written to the screen literally and NO byte is interpreted — not even ESC. So an
        // SGR color sequence must appear on screen as its raw bytes, not change any color state.
        write(terminal, CSI + "?7777h");
        assertTrue(terminal.currentPrivateModeState.XT_RAW_PASSTHROUGH, "mode is on");
        write(terminal, ESC + "[31m");    // would set fg=red if interpreted

        // The 5 bytes ESC [ 3 1 m render as glyphs: ESC->^[ (cols 0,1), then [ 3 1 m (cols 2-4).
        assertEquals('^', charAt(0, 0), "ESC renders as ^ (caret notation)");
        assertEquals('[', charAt(1, 0), "...then the [ half of ^[");
        assertEquals('[', charAt(2, 0), "the literal [ byte of the SGR renders next");
        assertEquals('3', charAt(3, 0));
        assertEquals('1', charAt(4, 0));
        assertEquals('m', charAt(5, 0));
        assertEquals(TerminalColors.ColorMode.DEFAULT_FOREGROUND, terminal.currentForegroundColorMode,
            "the SGR sequence must NOT have been interpreted — color mode unchanged");
    }

    @Test
    void xtRawPassthroughToggleOffResumesInterpretation() {
        // The toggle-off CSI ?7777l is matched literally while passthrough is on (the one sequence
        // interpreted in the mode) so the debugger can always be exited. After it, escapes are
        // honored again. 'XY' renders first; the exit sequence is consumed (not rendered); then
        // an SGR is interpreted normally.
        write(terminal, CSI + "?7777h");
        assertTrue(terminal.currentPrivateModeState.XT_RAW_PASSTHROUGH);
        write(terminal, "XY");             // renders literally at cols 0,1
        write(terminal, CSI + "?7777l");    // the exit sequence — matched, mode turns off
        assertFalse(terminal.currentPrivateModeState.XT_RAW_PASSTHROUGH, "mode is off");
        write(terminal, ESC + "[31m");      // now interpreted: sets fg = red
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode,
            "after toggling off, SGR is interpreted again");
        assertEquals('X', charAt(0, 0), "the 'XY' rendered before the exit sequence survives");
    }

    @Test
    void cbtMovesCursorToPreviousTabStop() {
        // CSI Z (CBT, Cursor Backward Tabulation) moves left to the previous tab stop, repeated
        // Ps times (default 1), clamping at column 0. Default tab stops are every 8 columns.
        write(terminal, CSI + "1;25H");   // col 25 (x=24)
        write(terminal, CSI + "Z");        // CBT 1 -> back to tab stop at col 17 (x=16)
        assertEquals(16, terminal.x, "CBT from col 24 lands on the tab stop at col 16");

        write(terminal, CSI + "1;1H" + CSI + "30G");  // col 30 (x=29)
        write(terminal, CSI + "3Z");      // CBT 3 -> back 3 tab stops: 24 -> 16 -> 8 (x=8)
        assertEquals(8, terminal.x, "CBT 3 from col 29 lands on the tab stop at col 8");
    }

    @Test
    void cbtClampsToColumnZero() {
        write(terminal, CSI + "1;3H");     // col 3 (x=2)
        write(terminal, CSI + "5Z");      // CBT 5 — past the left edge
        assertEquals(0, terminal.x, "CBT clamps to column 0, never off-screen left");
    }

    @Test
    void cursorMovesTreatExplicitZeroParamAsDefaultOne() {
        // CSI 0Z (explicit 0) must behave as CBT 1, not CBT 0 — the CSIManager default-applies
        // 0 -> 1 before the handler runs, so handlers can trust args[0] >= 1 and don't re-guard.
        // Covers CBT/CHT/REP/CNL/CPL/VPR/HPA/HPR (all default to 1); spot-checked with CBT + CHT.
        write(terminal, CSI + "1;25H");   // col 25 (x=24)
        write(terminal, CSI + "0Z");      // explicit 0 -> default 1 -> CBT to col 17 (x=16)
        assertEquals(16, terminal.x, "CSI 0Z (explicit 0) defaults to CBT 1");

        write(terminal, CSI + "1;1H" + CSI + "3G");  // col 3 (x=2)
        write(terminal, CSI + "0I");      // explicit 0 -> default 1 -> CHT to col 9 (x=8)
        assertEquals(8, terminal.x, "CSI 0I (explicit 0) defaults to CHT 1");
    }

    @Test
    void chtMovesCursorToNextTabStop() {
        // CSI I (CHT, Cursor Forward Tabulation) moves right to the next tab stop, repeated Ps
        // times (default 1), clamping at the last column. Mirror of CBT. Default stops every 8.
        write(terminal, CSI + "1;3H");     // col 3 (x=2)
        write(terminal, CSI + "I");        // CHT 1 -> next tab stop at col 9 (x=8)
        assertEquals(8, terminal.x, "CHT from col 2 lands on the tab stop at col 8");

        write(terminal, CSI + "1;1H" + CSI + "3G");  // col 3 (x=2)
        write(terminal, CSI + "3I");      // CHT 3 -> 8 -> 16 -> 24 (x=24)
        assertEquals(24, terminal.x, "CHT 3 from col 2 lands on the tab stop at col 24");
    }

    @Test
    void chtClampsToLastColumn() {
        write(terminal, CSI + "1;78H");    // near the right edge (x=77)
        write(terminal, CSI + "9I");      // CHT 9 — past the right edge
        assertEquals(Terminal.WIDTH - 1, terminal.x, "CHT clamps to the last column, never off-screen right");
    }

    @Test
    void cbtMovesCursorSoFollowingOverwriteHitsADistinctColumn() {
        // After positioning past the end of a line, a program may delete-by-overwriting: send CBT
        // to step the cursor back a tab stop, then a space to blank that cell, expecting each CBT
        // to land on a new column. Pre-fix CBT was a no-op, so the cursor never moved and every
        // space overwrote the SAME column — fewer characters were deleted than the program
        // expected. With CBT working, the cursor moves back and the overwrite hits a distinct cell.
        write(terminal, "ABCDEFGHI");     // 9 chars, cols 0..8
        write(terminal, CSI + "1;10H");    // cursor to col 10 (one past end, x=9)
        // CBT to the previous tab stop (col 9 -> col 8, x=8), then a space overwrites 'I'.
        write(terminal, CSI + "Z ");      // CBT to x=8, space overwrites 'I' at col 8
        assertEquals(' ', charAt(8, 0), "CBT+space deletes the char at the tab stop, not the cell the cursor was on");
        assertEquals(9, terminal.x, "after the space the cursor advanced past col 8 (x=8->9)");
    }

    @Test
    void cnlMovesDownAndToColumnZero() {
        // CSI E (CNL, Cursor Next Line) moves down Ps lines and to column 0.
        write(terminal, CSI + "5;10H");   // row 5, col 10 (x=9, y=4)
        write(terminal, CSI + "3E");      // CNL 3 -> row 8, col 0
        assertEquals(0, terminal.x, "CNL moves to column 0");
        assertEquals(7, terminal.y, "CNL 3 from row 4 lands on row 7");
    }

    @Test
    void cplMovesUpAndToColumnZero() {
        // CSI F (CPL, Cursor Previous Line) moves up Ps lines and to column 0.
        write(terminal, CSI + "10;15H");  // row 10, col 15 (x=14, y=9)
        write(terminal, CSI + "4F");      // CPL 4 -> row 6, col 0
        assertEquals(0, terminal.x, "CPL moves to column 0");
        assertEquals(5, terminal.y, "CPL 4 from row 9 lands on row 5");
    }

    @Test
    void vprMovesRowRelativeKeepingColumn() {
        // CSI e (VPR, Vertical Position Relative) moves down Ps rows, keeping the column.
        write(terminal, CSI + "3;12H");  // row 3, col 12 (x=11, y=2)
        write(terminal, CSI + "5e");     // VPR 5 -> row 8, col 12
        assertEquals(11, terminal.x, "VPR keeps the column");
        assertEquals(7, terminal.y, "VPR 5 from row 2 lands on row 7");
    }

    @Test
    void hpaMovesColumnAbsoluteKeepingRow() {
        // CSI ` (HPA, Horizontal Position Absolute) moves to column Ps (1-based), keeping row.
        write(terminal, CSI + "4;20H");  // row 4, col 20 (x=19, y=3)
        write(terminal, CSI + "8`");     // HPA 8 -> col 8 (x=7), row 4
        assertEquals(7, terminal.x, "HPA moves to column Ps-1");
        assertEquals(3, terminal.y, "HPA keeps the row");
    }

    @Test
    void hprMovesColumnRelativeKeepingRow() {
        // CSI a (HPR, Horizontal Position Relative) moves right Ps columns, keeping row.
        write(terminal, CSI + "2;5H");   // row 2, col 5 (x=4, y=1)
        write(terminal, CSI + "10a");    // HPR 10 -> col 15 (x=14), row 2
        assertEquals(14, terminal.x, "HPR moves right Ps columns");
        assertEquals(1, terminal.y, "HPR keeps the row");
    }

    @Test
    void rcpRestoresCursorSavedByScp() {
        // CSI u (RCP) restores the full saved state (position + rendition) saved by CSI s
        // (SCOSC) — the same scope as DECRC, matching xterm-410 (SCORC uses DECSC_FLAGS, not
        // position-only). Save position AND a color, change both, RCP brings both back.
        write(terminal, CSI + "6;12H");  // row 6, col 12 (x=11, y=5)
        write(terminal, CSI + "31m");    // fg = red
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode);
        write(terminal, CSI + "s");      // SCOSC — save cursor (full state)
        write(terminal, CSI + "1;1H");   // move to home (x=0, y=0)
        write(terminal, CSI + "32m");    // change fg to green
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode);
        write(terminal, CSI + "u");      // SCORC — restore
        assertEquals(11, terminal.x, "RCP restores the saved column");
        assertEquals(5, terminal.y, "RCP restores the saved row");
        // The saved rendition must round-trip, not just position: after setting green post-save,
        // RCP restores the saved SIXTEEN_COLOR mode (and its palette copy). Foreground color
        // mode is the field that actually changes across SGR 31/32 and round-trips through
        // SavedCursor, so it's the meaningful check that RCP restores rendition, not position-only.
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode,
            "RCP restores the saved foreground color mode (rendition), not just position");
    }

    @Test
    void scpAndDecscShareSavedStateSoRcpRestoresEither() {
        // SCP (CSI s) and DECSC (ESC 7) save to the same state via the unified SavedCursor, so
        // a save by one and a restore by the other's alias must work.
        write(terminal, CSI + "4;8H" + ESC + "7");   // DECSC at row 4 col 8
        write(terminal, CSI + "1;1H");
        write(terminal, CSI + "u");                   // RCP (the SCORC alias) restores it
        assertEquals(7, terminal.x, "DECSC save + RCP restore: column");
        assertEquals(3, terminal.y, "DECSC save + RCP restore: row");
    }

    @Test
    void decscDecrcPreservesAutowrapPendingAcrossSaveRestore() {
        // xterm saves/restores do_wrap as part of the cursor (CursorSave2 stores wrap_flag;
        // CursorRestoreFlags sets do_wrap = sc->wrap_flag AFTER CursorSet/ResetWrap). So a cursor
        // saved mid-pending-wrap must restore still pending. Pre-fix, restore cleared the
        // pending flag (via setCursorPos) and never restored it.
        write(terminal, "A".repeat(Terminal.WIDTH));  // fill row 0 -> autowrapPending armed
        assertTrue(terminal.autowrapPending, "precondition: pending is armed at the last column");
        write(terminal, ESC + "7");                  // DECSC — save (captures pending=true)
        // Move the cursor (clears pending) to prove the restore brings pending back, not the
        // ambient state.
        write(terminal, CSI + "1;1H");
        assertFalse(terminal.autowrapPending, "a cursor move clears pending before the restore");
        write(terminal, ESC + "8");                  // DECRC — restore
        assertTrue(terminal.autowrapPending,
            "DECRC must restore the saved autowrap-pending flag (xterm do_wrap), not clear it");
        // The cursor position is restored too — and pending means the cursor sits at width-1.
        assertEquals(Terminal.WIDTH - 1, terminal.x,
            "the restored cursor sits at the last column (pending), not the home we moved to");
    }

    @Test
    void decrcClampsSavedCursorAfterWidthShrink() {
        // §36 m1: DECRC/restoreSavedCursor didn't clamp after a width change — ESC7 in 132 cols
        // at col 100 → ?3l (setWidth homes but doesn't reset savedX) → ESC8 gave x=100 > 79 →
        // OOB → false wrap on the next char. Fix: clamp at restore. Our SavedCursor.restore
        // routes through setCursorPos (Math.clamp), so the saved 132-space coordinate clamps to
        // the 80-space last column and the next char does NOT false-wrap.
        write(terminal, CSI + "?3h");           // DECCOLM 132
        assertEquals(132, terminal.getTerminalWidth());
        write(terminal, CSI + "1;100H");        // CUP to column 100 (x=99), valid in 132
        write(terminal, ESC + "7");             // DECSC — save savedX=99
        write(terminal, CSI + "?3l");            // DECCOLM 80 — setWidth(80), homes cursor
        assertEquals(Terminal.WIDTH, terminal.getTerminalWidth());
        assertEquals(0, terminal.x, "setWidth homes the cursor");
        write(terminal, ESC + "8");             // DECRC — restore
        assertTrue(terminal.x <= Terminal.WIDTH - 1,
            "restored x must clamp to the 80-col last column (79), not the saved 132-col 99");
        assertEquals(79, terminal.x, "clamps to width-1");
        write(terminal, "X");                   // next char — must NOT false-wrap
        assertEquals(0, terminal.y, "no false wrap to the next row");
    }

    @Test
    void repRepeatsLastPrintedChar() {
        // CSI b (REP) repeats the preceding printable char Ps times via putChar, so the repeats
        // advance the cursor and wrap like real input.
        write(terminal, "A");            // print 'A' at col 0 (x=1)
        write(terminal, CSI + "4b");     // REP 4 -> four more 'A's (cols 1-4)
        assertEquals('A', charAt(0, 0));
        assertEquals('A', charAt(4, 0), "REP filled cols 1-4 with the repeated char");
        assertEquals(5, terminal.x, "after 'A' + REP 4 the cursor is at col 5");
    }

    @Test
    void repIsNoOpAfterCursorMove() {
        // A cursor move clears the "last printed char" (xterm lastchar), so REP with nothing to
        // repeat is a no-op.
        write(terminal, "A");
        write(terminal, CSI + "1;1H");   // home — clears lastPrintedChar
        write(terminal, CSI + "3b");     // REP 3 — nothing to repeat
        assertEquals('A', charAt(0, 0), "the original 'A' is untouched");
        assertEquals(' ', charAt(1, 0), "REP after a cursor move writes nothing");
    }

    @Test
    void repClampsHugeCountToOneScreen() {
        // parseArgument saturates at Integer.MAX_VALUE, so CSI 2147483647b would loop ~2^31 times
        // (each a full putChar inside the IO lock) and freeze the VM. REP must clamp: capping at
        // one screen (width * HEIGHT) means the repeats fill the screen and scroll, but return.
        write(terminal, "X");
        final long start = System.nanoTime();
        write(terminal, CSI + "2147483647b");
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        assertTrue(elapsedMs < 5000, "REP MAX_VALUE must not freeze — clamped to one screen");
        // The screen is full of 'X' (clamped count fills exactly width*HEIGHT cells, scrolling
        // the original; the visible window is all 'X').
        assertEquals('X', charAt(0, 0), "the clamped REP fills the visible screen with the char");
    }

    @Test
    void hpaKeepsRowUnderDecomOriginMode() {
        // §36 review: HPA used setRelativeCursorPos, which under DECOM adds scrollFirst to the
        // (absolute) terminal.y — shifting the row. HPA keeps the row; only the column changes.
        // Fix: use setClampedCursorPos like the sibling CHA.
        write(terminal, CSI + "3;8r");        // scroll region rows 3-8 (0-indexed 2..7)
        write(terminal, CSI + "?6h");         // DECOM on (origin mode)
        write(terminal, CSI + "5;3H");        // CUP row 5 col 3 -> under DECOM, y = scrollFirst+4 = 6
        assertEquals(6, terminal.y, "precondition: DECOM puts row 5 at absolute y=6");
        write(terminal, CSI + "20`");         // HPA 20 -> column 19, row must STAY at 6
        assertEquals(19, terminal.x, "HPA sets the column");
        assertEquals(6, terminal.y, "HPA must not shift the row under DECOM");
    }

    @Test
    void xtrestoreQuestionMarkURestoresSavedPrivateMode() {
        // CSI ?Ps u is XTRESTORE — the mirror of CH6's ?s XTSAVE. It restores private mode Ps
        // from savePrivateModeState into currentPrivateModeState. CH12 groups u = SCORC (plain)
        // + ?u (XTRESTORE) like CH6 groups s = SCOSC + ?s (XTSAVE). This replaces the earlier
        // "?u doesn't hijack RCP" guard: the ?u branch is now a real handler, not a no-op.
        // Use DECCOLM (?3) as the mode: XTSAVE it while on, turn it off, XTRESTORE brings it back.
        write(terminal, CSI + "?3h");            // DECCOLM on
        assertTrue(terminal.currentPrivateModeState.DECCOLM, "precondition: DECCOLM on");
        write(terminal, CSI + "?3s");            // XTSAVE mode 3 -> save DECCOLM=true
        write(terminal, CSI + "?3l");            // turn DECCOLM off
        assertFalse(terminal.currentPrivateModeState.DECCOLM, "DECCOLM off before restore");
        write(terminal, CSI + "?3u");            // XTRESTORE mode 3 -> restore DECCOLM=true
        assertTrue(terminal.currentPrivateModeState.DECCOLM,
            "XTRESTORE (?u) restores the saved private mode (mirror of ?s XTSAVE)");
    }

    @Test
    void scorcPlainUDoesNotTouchSavedPrivateModes() {
        // Plain CSI u (SCORC) restores the cursor only — it must not touch the private modes
        // (that's ?u / XTRESTORE). And ?u must not restore the cursor (that's plain u / SCORC).
        // The two branches of CH12 are cleanly separated by the questionMark.
        write(terminal, CSI + "?3h");            // DECCOLM on
        write(terminal, CSI + "1;10H");          // cursor at col 10
        write(terminal, ESC + "7");              // DECSC — save cursor
        write(terminal, CSI + "1;1H");            // move to home
        write(terminal, CSI + "u");               // SCORC (plain u) — restore cursor only
        assertEquals(9, terminal.x, "plain u (SCORC) restores the cursor");
        assertTrue(terminal.currentPrivateModeState.DECCOLM,
            "plain u (SCORC) does not touch private modes — DECCOLM stays on");
    }

    @Test
    void deccolmSwitchesColumnWidthAndClearsScreen() {
        assertEquals(Terminal.WIDTH, terminal.getTerminalWidth(), "default is 80 columns");

        // DECCOLM set (?3h): switch to 132 columns, clear screen, reset margins, home cursor.
        resetDirty();
        write(terminal, CSI + "?3h");
        assertTrue(terminal.currentPrivateModeState.DECCOLM, "?3h enables DECCOLM");
        assertEquals(132, terminal.getTerminalWidth(), "DECCOLM switches to 132 columns");
        final int expected132 = 132 * Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT;
        assertEquals(expected132, terminal.buffer.length, "buffers reallocate to 132 columns");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF,
            "DECCOLM must redraw the whole screen");
        assertEquals(0, terminal.x, "DECCOLM homes the cursor to column 0");
        assertEquals(0, terminal.y, "DECCOLM homes the cursor to row 0");

        // 132 chars fit on row 0 with no wrap — proves putChar wraps at the dynamic
        // width, not the static Terminal.WIDTH (which would wrap at 80 and spill to row 1).
        write(terminal, "A".repeat(132));
        assertEquals('A', charAt(131, 0), "last column of the 132-col row is filled");
        assertEquals(' ', charAt(0, 1), "no wrap to row 1 at 132 columns");

        // A char placed on row 1 must use the 132-column stride, not the 80-column one.
        write(terminal, CSI + "2;6H");   // CUP -> line 2, column 6 (x=5, y=1)
        write(terminal, "Z");
        assertEquals('Z', charAt(5, 1), "row 1 indexing uses the 132-column stride");

        // DECCOLM reset (?3l): switch back to 80 columns.
        write(terminal, CSI + "?3l");
        assertFalse(terminal.currentPrivateModeState.DECCOLM, "?3l disables DECCOLM");
        assertEquals(Terminal.WIDTH, terminal.getTerminalWidth(), "reset returns to 80 columns");
        final int expected80 = Terminal.WIDTH * Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT;
        assertEquals(expected80, terminal.buffer.length, "buffers reallocate back to 80 columns");
    }

    @Test
    void risResetsColumnWidthToEighty() {
        // DECCOLM to 132, then RIS (ESC c, full reset) must return to the 80-column power-on
        // default. RIS builds a fresh PrivateModeState (so the DECCOLM flag reads false) but must
        // also reset the allocated width — otherwise the flag and the buffer width silently diverge.
        write(terminal, CSI + "?3h");
        assertEquals(132, terminal.getTerminalWidth(), "precondition: DECCOLM switches to 132");

        write(terminal, ESC + "c");   // RIS
        assertFalse(terminal.currentPrivateModeState.DECCOLM, "RIS clears the DECCOLM flag");
        assertEquals(Terminal.WIDTH, terminal.getTerminalWidth(),
            "RIS must reset the column width to the 80-column default, not leave it at 132");
    }

    @Test
    void deccolmResetsSgrAttributesAndErasesToDefaultBackground() {
        // DECCOLM (VT100–VT420) is a destructive reset: it clears SGR attributes and erases the
        // screen to the DEFAULT background, not the SGR background that was active. Contrast with
        // ECH (echErasedCellsTakeDefaultForegroundAndCurrentBackground), which keeps current bg.
        write(terminal, CSI + "41m");    // bg = SIXTEEN_COLOR red (sixteenColor.g = 1)
        write(terminal, SAMPLE_LINE);
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentBackgroundColorMode,
            "precondition: SGR bg is set");

        write(terminal, CSI + "?3h");    // DECCOLM -> 132 columns, destructive reset
        assertEquals(TerminalColors.ColorMode.DEFAULT_BACKGROUND, terminal.currentBackgroundColorMode,
            "DECCOLM resets the background color mode to default");
        assertEquals(TerminalColors.ColorMode.DEFAULT_FOREGROUND, terminal.currentForegroundColorMode,
            "DECCOLM resets the foreground color mode to default");
        assertEquals(TerminalColors.DEFAULT_STYLE, terminal.style,
            "DECCOLM resets SGR attributes (style)");

        final int idx = cellIndex(0, 0);
        assertEquals(TerminalColors.ColorMode.DEFAULT_BACKGROUND, terminal.colorsBackground[idx].mode,
            "cleared cell background must be the DEFAULT background, not the prior SGR red");
        assertEquals(' ', (char) terminal.buffer[idx],
            "cleared cell must be a space");
    }

    private void write(final Terminal target, final String text) {
        target.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    private void fillRows(final int startRow, final String chars) {
        for (int i = 0; i < chars.length(); i++) {
            write(terminal, CSI + (startRow + 1 + i) + ";1H" + chars.charAt(i));
        }
    }

    private char charAt(final int x, final int y) {
        final int row = y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
        return (char) terminal.buffer[x + row * terminal.width];
    }

    private int cellIndex(final int x, final int y) {
        final int row = y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
        return x + row * terminal.width;
    }

    private char altCharAt(final int x, final int y) {
        return (char) terminal.altBuffer[x + y * Terminal.WIDTH];
    }

    private void resetDirty() {
        renderer.dirtyMask.set(0);
    }

    private static final class DummyRenderer implements RendererModel {
        private final AtomicInteger dirtyMask = new AtomicInteger();

        @Override
        public AtomicInteger getDirtyMask() {
            return dirtyMask;
        }

        @Override
        public void close() {
            dirtyMask.set(0);
        }
    }

    @Test
    void dirtyMaskScrollDownAfterScrollbackGrowth() {
        // Loki's repro: 30 line-feeds grow lastRowToDisplayMax, then CSI 1 T (scroll down)
        // must mark ALL visible rows dirty. The pre-fix code mapped buffer rows to screen
        // rows with lastRowToDisplayMax instead of lastRowToDisplay, leaving rows 0..6 stale.
        for (int i = 0; i < 30; i++) {
            write(terminal, "\n");
        }
        assertTrue(terminal.lastRowToDisplayMax > Terminal.HEIGHT,
            "lastRowToDisplayMax should exceed HEIGHT after 30 line-feeds");

        resetDirty();
        write(terminal, CSI + "1T"); // scroll down 1 line
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF,
            "Scroll down after scrollback growth must mark all 24 visible rows dirty");
    }

    @Test
    void dirtyMaskScrollUpInMarginAfterScrollbackGrowth() {
        // Second half of the same regression: set a margin AFTER scrollback has grown, then
        // scroll up within it. The margin rows (not buffer rows) must be the dirty bits.
        for (int i = 0; i < 30; i++) {
            write(terminal, "\n");
        }
        write(terminal, CSI + "2;8r"); // scroll region rows 1..7 (0-indexed)
        resetDirty();
        write(terminal, CSI + "1S");  // scroll up 1 within the margin
        int expected = 0;
        for (int i = 1; i <= 7; i++) {
            expected |= (1 << i);
        }
        assertEquals(expected, renderer.dirtyMask.get() & 0xFF,
            "Margin scroll after scrollback growth must mark the margin rows, not stale buffer rows");
    }

    @Test
    void scrollDownAtFullScrollbackDoesNotOverflow() {
        // Blocker B1: fill scrollback to the cap (lastRowToDisplayMax == 480), then
        // SD (CSI T) / RI used to arraycopy past the physical buffer end
        // (AIOOBE under lock in putOutput -> terminal dead forever).
        writeMarkers();
        assertEquals(Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT, terminal.lastRowToDisplayMax,
            "precondition: scrollback filled to cap");
        // Marker two rows above the discarded pair: survives the shift onto the last row.
        final int bottomMarkerRow = Terminal.HEIGHT - 3;
        write(terminal, CSI + (bottomMarkerRow + 1) + ";1HZ");

        assertDoesNotThrow(() -> write(terminal, CSI + "2T"));
        // Lines pushed off the bottom are discarded, the two top rows are blank.
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(0, 1));
        // The marker sat directly above the discarded rows: shifted down onto the last row.
        assertEquals('Z', charAt(0, Terminal.HEIGHT - 1));
    }

    @Test
    void reverseIndexAtFullScrollbackDoesNotOverflow() {
        // Same B1 path via RI (ESC M), the second trigger named in the audit repro.
        writeMarkers();
        assertDoesNotThrow(() -> write(terminal, ESC + "M"));
    }

    @Test
    void scrollUpDownWithHugeCountTerminates() {
        // Blocker B2: EscapeUtilities.parseArgument saturates at Integer.MAX_VALUE;
        // SU/SD loops must clamp instead of freezing the VM output thread.
        writeMarkers();
        long start = System.nanoTime();
        assertDoesNotThrow(() -> write(terminal, CSI + "2147483647S"));
        assertDoesNotThrow(() -> write(terminal, CSI + "999999999T"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        assertTrue(elapsedMs < 5000, "SU/SD with saturated count took " + elapsedMs + "ms");
        // Everything scrolled out; the whole visible window must be blank.
        for (int y = 0; y < Terminal.HEIGHT; y++) {
            assertEquals(' ', charAt(0, y), "row " + y + " should be blank after huge SU");
        }
    }

    private void writeMarkers() {
        // Grow the scrollback window to its hard cap (HEIGHT * SCROLL_BACK_COUNT rows).
        final StringBuilder feed = new StringBuilder();
        feed.append("\n".repeat(Terminal.HEIGHT * terminal.SCROLL_BACK_COUNT));
        write(terminal, feed.toString());
    }
}