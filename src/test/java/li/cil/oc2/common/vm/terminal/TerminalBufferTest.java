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
        write(terminal, CSI + "41m");    // bg = SIXTEEN_COLOR red (sixteenColor.G = 1)
        write(terminal, SAMPLE_LINE);
        write(terminal, CSI + "3G");     // x=2
        write(terminal, CSI + "2X");     // ECH 2
        final int idx = cellIndex(2, 0);
        assertEquals(TerminalColors.ColorMode.DEFAULT_FOREGROUND, terminal.colors[idx].Mode,
            "erased cell foreground must be the DEFAULT_FOREGROUND marker");
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.colorsBackground[idx].Mode,
            "erased cell background must keep the current bg mode");
        assertEquals(1, terminal.colorsBackground[idx].G,
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
    void cbtLetsNanosBacktabOverwriteDeleteAcrossColumns() {
        // The nano repro: after positioning at end of a line, nano sends CBT then a space to
        // delete-by-overwriting, expecting each CBT to move left so each space hits a new column.
        // Pre-fix CBT was a no-op, so every space overwrote the SAME column -> "fewer chars
        // deleted than expected". With CBT working, the cursor moves and each space deletes a
        // distinct char.
        write(terminal, "ABCDEFGHI");     // 9 chars, cols 0..8
        write(terminal, CSI + "1;10H");    // cursor to col 10 (one past end, x=9)
        // Simulate nano: CBT to the previous tab stop (col 9 -> col 8, x=8), write a space to
        // delete 'I'; CBT again is col 8 -> col 0 (next prev stop below 8 is 0), write space.
        write(terminal, CSI + "Z ");      // CBT to x=8, space overwrites 'I' at col 8
        assertEquals(' ', charAt(8, 0), "first CBT+space deletes the char at col 8");
        assertEquals(9, terminal.x, "after the space the cursor advanced past col 8 (x=8->9)");
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
        write(terminal, CSI + "41m");    // bg = SIXTEEN_COLOR red (sixteenColor.G = 1)
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
        assertEquals(TerminalColors.ColorMode.DEFAULT_BACKGROUND, terminal.colorsBackground[idx].Mode,
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