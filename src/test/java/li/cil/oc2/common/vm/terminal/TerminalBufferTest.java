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
    void repeatedDeccolmSetDoesNotClearScreen() {
        // xterm only performs the DECCOLM clear/home on an actual width change; a redundant
        // ?3h while already at 132 columns must be a no-op, not a second screen wipe.
        write(terminal, CSI + "?3h");
        write(terminal, CSI + "2;1H" + "X");
        final int[] bufferBefore = terminal.buffer.clone();

        write(terminal, CSI + "?3h");
        assertTrue(terminal.currentPrivateModeState.DECCOLM, "flag stays set");
        assertEquals(132, terminal.getTerminalWidth(), "width unchanged");
        assertArrayEquals(bufferBefore, terminal.buffer,
            "redundant ?3h must not reallocate or clear the buffers");
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
        return x + row * Terminal.WIDTH;
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
}