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
        write(terminal, "ABCDEFGH\u001b[2;1HXYZ");
        buffer.clearLine(0);
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals('X', charAt(0, 1));
    }

    @Test
    void shiftUpOneMovesRows() {
        write(terminal, "\u001b[2;8r");
        fillRows(1, "ABCDEFG");
        buffer.shiftUpOne();
        assertEquals(' ', charAt(0, 0));
        assertEquals('B', charAt(0, 1));
        assertEquals('G', charAt(0, 6));
        assertEquals(' ', charAt(0, 7));
    }

    @Test
    void shiftDownOneMovesRows() {
        write(terminal, "\u001b[2;8r");
        fillRows(1, "ABCDEFG");
        buffer.shiftDownOne();
        assertEquals(' ', charAt(0, 1));
        assertEquals('A', charAt(0, 2));
        assertEquals('F', charAt(0, 7));
        assertEquals(' ', charAt(0, 0));
    }

    @Test
    void clearAllLines() {
        write(terminal, "ABCDEFGH\u001b[2;1HXYZ");
        buffer.clear();
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(' ', charAt(0, 1));
    }

    @Test
    void cupMovesCursor() {
        write(terminal, "\u001b[3;4H");
        assertEquals(3, terminal.x);
        assertEquals(2, terminal.y);
        write(terminal, "\u001b[5;6f");
        assertEquals(5, terminal.x);
        assertEquals(4, terminal.y);
        write(terminal, "\u001b[999;999H");
        assertEquals(Terminal.WIDTH - 1, terminal.x);
        assertEquals(Terminal.HEIGHT - 1, terminal.y);
        write(terminal, "\u001b[H");
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
    }

    @Test
    void chaMovesColumnOnly() {
        write(terminal, "\u001b[3;4H\u001b[40G");
        assertEquals(39, terminal.x);
        assertEquals(2, terminal.y);
        write(terminal, "\u001b[G");
        assertEquals(0, terminal.x);
    }

    @Test
    void vpaMovesRowOnly() {
        write(terminal, "\u001b[3;4H\u001b[7d");
        assertEquals(3, terminal.x);
        assertEquals(6, terminal.y);
    }

    @Test
    void edClearsFromCursorToEndOfScreen() {
        write(terminal, "ABCDEFGH\u001b[3G\u001b[J");
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        assertEquals(' ', charAt(2, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(2, terminal.x);
        assertEquals(0, terminal.y);
    }

    @Test
    void edClearsFromStartOfScreenToCursor() {
        write(terminal, "ABCDEFGH\u001b[5G\u001b[1J");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(4, 0));
        assertEquals('F', charAt(5, 0));
        assertEquals('H', charAt(7, 0));
    }

    @Test
    void edClearsWholeScreenWithoutMovingCursor() {
        write(terminal, "ABCDEFGH\u001b[6;11H\u001b[2J");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(' ', charAt(0, 5));
        assertEquals(10, terminal.x);
        assertEquals(5, terminal.y);
    }

    @Test
    void elClearsFromCursorToEndOfLine() {
        write(terminal, "ABCDEFGH\u001b[5G\u001b[K");
        assertEquals('A', charAt(0, 0));
        assertEquals('D', charAt(3, 0));
        assertEquals(' ', charAt(4, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(4, terminal.x);
    }

    @Test
    void elClearsFromStartOfLineToCursor() {
        write(terminal, "ABCDEFGH\u001b[5G\u001b[1K");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(4, 0));
        assertEquals('F', charAt(5, 0));
        assertEquals('H', charAt(7, 0));
    }

    @Test
    void elClearsWholeLine() {
        write(terminal, "ABCDEFGH\u001b[2K");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(7, 0));
        assertEquals(8, terminal.x);
    }

    @Test
    void decstbmSetsMarginsAndHomesCursor() {
        write(terminal, "\u001b[3;8r");
        assertEquals(2, terminal.scrollFirst);
        assertEquals(7, terminal.scrollLast);
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
        write(terminal, "\u001b[2;3r");
        assertEquals(1, terminal.scrollFirst);
        assertEquals(2, terminal.scrollLast);
    }

    @Test
    void decstbmIgnoresDegenerateMargins() {
        write(terminal, "\u001b[5;5r");
        assertEquals(0, terminal.scrollFirst);
        assertEquals(Terminal.HEIGHT - 1, terminal.scrollLast);
        write(terminal, "\u001b[10;3r");
        assertEquals(0, terminal.scrollFirst);
        assertEquals(Terminal.HEIGHT - 1, terminal.scrollLast);
    }

    @Test
    void decomOriginModeClampsCursorToMargins() {
        write(terminal, "\u001b[3;8r\u001b[?6h");
        assertTrue(terminal.currentPrivateModeState.DECOM);
        assertEquals(0, terminal.x);
        assertEquals(2, terminal.y);
        write(terminal, "\u001b[4;4H");
        assertEquals(3, terminal.x);
        assertEquals(5, terminal.y);
        write(terminal, "\u001b[5d");
        assertEquals(3, terminal.x);
        assertEquals(6, terminal.y);
        write(terminal, "\u001b[?6l");
        assertFalse(terminal.currentPrivateModeState.DECOM);
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
    }

    @Test
    void insertLinesShiftsContentDownWithinMargins() {
        write(terminal, "\u001b[2;8r");
        fillRows(1, "ABCDEFG");
        write(terminal, "\u001b[4;1H\u001b[2L");
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
        write(terminal, "\u001b[2;8r");
        fillRows(1, "ABCDEFG");
        write(terminal, "\u001b[4;1H\u001b[2M");
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
        write(terminal, "\u001b[2;8r");
        fillRows(1, "ABCDEFG");
        write(terminal, "\u001b[2S");
        assertEquals(' ', charAt(0, 0));
        assertEquals('C', charAt(0, 1));
        assertEquals('D', charAt(0, 2));
        assertEquals('G', charAt(0, 5));
        assertEquals(' ', charAt(0, 6));
        assertEquals(' ', charAt(0, 7));
    }

    @Test
    void scrollDownMovesContentDownWithinMargins() {
        write(terminal, "\u001b[2;8r");
        fillRows(1, "ABCDEFG");
        write(terminal, "\u001b[2T");
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
        write(terminal, "\u001b[1S");
        assertEquals('B', charAt(0, 0));
        assertEquals('C', charAt(0, 1));
        assertEquals('D', charAt(0, 2));
        assertEquals(' ', charAt(0, 3));
        assertEquals(25, terminal.lastRowToDisplayMax);
    }

    @Test
    void scrollUpCountMovesMultipleLines() {
        fillRows(0, "ABCD");
        write(terminal, "\u001b[2S");
        assertEquals('C', charAt(0, 0));
        assertEquals('D', charAt(0, 1));
        assertEquals(' ', charAt(0, 2));
        assertEquals(26, terminal.lastRowToDisplayMax);
    }

    @Test
    void scrollDownCountMovesMultipleLines() {
        fillRows(1, "ABCD");
        write(terminal, "\u001b[2T");
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
        write(terminal, "\u001b[?47h");
        assertTrue(terminal.currentPrivateModeState.isAltBufferEnabled());
        write(terminal, "World");
        assertEquals('H', charAt(0, 0));
        assertEquals('W', altCharAt(0, 0));
        assertEquals(' ', altCharAt(5, 0));
        write(terminal, "\u001b[?47l");
        assertFalse(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals('H', charAt(0, 0));
    }

    @Test
    void altBuffer1047Switches() {
        write(terminal, "Hello");
        write(terminal, "\u001b[?1047h");
        assertTrue(terminal.currentPrivateModeState.isAltBufferEnabled());
        write(terminal, "abc");
        assertEquals('H', charAt(0, 0));
        assertEquals('a', altCharAt(0, 0));
        write(terminal, "\u001b[?1047l");
        assertFalse(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals('H', charAt(0, 0));
    }

    @Test
    void altBuffer1049SavesAndRestoresCursor() {
        write(terminal, "A\u001b[5;6H");
        write(terminal, "\u001b[?1049h");
        assertTrue(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals(0, terminal.x);
        assertEquals(0, terminal.y);
        write(terminal, "B\u001b[10;10H");
        assertEquals(9, terminal.x);
        assertEquals(9, terminal.y);
        write(terminal, "\u001b[?1049l");
        assertFalse(terminal.currentPrivateModeState.isAltBufferEnabled());
        assertEquals(5, terminal.x);
        assertEquals(4, terminal.y);
        assertEquals('A', charAt(0, 0));
    }

    @Test
    void altBufferMarksScreenDirtyOnSwitch() {
        resetDirty();
        write(terminal, "\u001b[?47h");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
        resetDirty();
        write(terminal, "\u001b[?47l");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
        resetDirty();
        write(terminal, "\u001b[?1049h");
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
        write(terminal, "\u001b[?7l");
        write(terminal, "A".repeat(Terminal.WIDTH) + "B");
        assertEquals('B', charAt(Terminal.WIDTH - 1, 0));
        assertEquals(' ', charAt(0, 1));
        assertEquals(0, terminal.y);
    }

    @Test
    void dirtyMaskMarksScreenOnScroll() {
        resetDirty();
        write(terminal, "\u001b[2S");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF);
    }

    @Test
    void dirtyMaskMarksMarginRowsOnScroll() {
        write(terminal, "\u001b[2;8r");
        fillRows(1, "ABCDEFG");
        resetDirty();
        write(terminal, "\u001b[2S");
        assertEquals(0b11111110, renderer.dirtyMask.get() & 0xFF);
    }

    @Test
    void dirtyMaskMarksClearedLine() {
        write(terminal, "X\u001b[5;1H");
        resetDirty();
        write(terminal, "\u001b[K");
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
        write(terminal, "\u001b[5;10r");
        assertEquals(4, terminal.scrollFirst);
        assertEquals(9, terminal.scrollLast);
        // Move cursor to row 1 (0-indexed: 0), which is outside the scroll region
        write(terminal, "\u001b[1;1H");
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
        write(terminal, "\u001b[5;10r");
        // Move cursor into the scroll region
        write(terminal, "\u001b[7;1H");
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
        write(terminal, "\u001b[?7l");        // DECAWM off
        write(terminal, "\u001b[?6h");        // DECOM on
        write(terminal, "\u001b[?7s");        // XTSAVE mode 7 (DECAWM)
        write(terminal, "\u001b[?6s");        // XTSAVE mode 6 (DECOM)
        // Verify savePrivateModeState captured the modified values
        assertFalse(terminal.savePrivateModeState.DECAWM, "DECAWM should be saved as off");
        assertTrue(terminal.savePrivateModeState.DECOM, "DECOM should be saved as on");
        // Now RIS
        write(terminal, "\u001bc");
        // savePrivateModeState should be reset to defaults
        assertTrue(terminal.savePrivateModeState.DECAWM, "DECAWM should be default (on) after RIS");
        assertFalse(terminal.savePrivateModeState.DECOM, "DECOM should be default (off) after RIS");
    }

    // --- RIS resets terminal.state to NORMAL ---

    @Test
    void risResetsTerminalStateToNormal() {
        // Put the terminal into an escape state by sending ESC followed by a non-completing char
        write(terminal, "\u001b[3");
        // We should be in CONTROL_SEQUENCE state (partial CSI)
        assertEquals(Terminal.State.CONTROL_SEQUENCE, terminal.state);
        // Now RIS
        write(terminal, "\u001bc");
        assertEquals(Terminal.State.NORMAL, terminal.state);
    }

    @Test
    void risResetsStateFromEscape() {
        // Put terminal into ESCAPE state
        write(terminal, "\u001b");
        assertEquals(Terminal.State.ESCAPE, terminal.state);
        // RIS
        write(terminal, "\u001bc");
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
        write(terminal, "\u001bc");
        assertEquals(-1, terminal.io.readInput(), "input queue should be empty after RIS");
    }

    // --- getDirtyRow refactor: scrolling main buffer with scrollback marks correct dirty lines ---

    @Test
    void dirtyMaskScrollMainBufferWithScrollback() {
        // Fill rows 0-3 with distinct content (the "nano bug" scenario)
        fillRows(0, "ABCD");
        // Scroll up 1 line — this grows lastRowToDisplayMax to 25
        resetDirty();
        write(terminal, "\u001b[1S");
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
        write(terminal, "\u001b[2S");
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
        write(terminal, "\u001b[1S"); // scroll up 1, lastRowToDisplayMax=25
        resetDirty();
        // Write a character at row 0 — should mark row 0 dirty
        write(terminal, "\u001b[1;1HZ");
        assertEquals(1, renderer.dirtyMask.get(), "Only row 0 should be dirty after writing to row 0");
        assertEquals('Z', charAt(0, 0));
    }

    @Test
    void dirtyMaskScrollUpInMarginRegionWithScrollback() {
        // Set scroll region [2..8] (0-indexed: 1..7)
        write(terminal, "\u001b[2;8r");
        fillRows(1, "ABCDEFG");
        // Scroll up 1 within the margin
        resetDirty();
        write(terminal, "\u001b[1S");
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
        write(terminal, "ABCDEFGH");
        write(terminal, "\u001b[3G");     // cursor to col 3 (x=2, the 'C')
        write(terminal, "\u001b[2X");     // ECH 2: erase 2 chars, no shift
        assertEquals('A', charAt(0, 0));
        assertEquals('B', charAt(1, 0));
        assertEquals(' ', charAt(2, 0), "ECH blanks the char at the cursor");
        assertEquals(' ', charAt(3, 0), "ECH blanks N chars from the cursor");
        assertEquals('E', charAt(4, 0), "ECH must not shift; later chars stay put");
        assertEquals('F', charAt(5, 0));
    }

    @Test
    void dchDeletesCharsShiftingLeft() {
        write(terminal, "ABCDEFGH");
        write(terminal, "\u001b[3G");     // x=2 (C)
        write(terminal, "\u001b[2P");     // DCH 2: delete 2, shift left, blank the tail
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
        write(terminal, "ABCDEFGH");
        write(terminal, "\u001b[3G");     // x=2 (C)
        write(terminal, "\u001b[2@");     // ICH 2: insert 2 blanks, shift right
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
        write(terminal, "\u001b[1G");     // x=0 (A)
        write(terminal, "\u001b[4h");     // IRM on (SM 4)
        write(terminal, "X");             // insert X at col 0; A,B shift right
        assertEquals('X', charAt(0, 0));
        assertEquals('A', charAt(1, 0), "IRM shifts existing chars right");
        assertEquals('B', charAt(2, 0));
        write(terminal, "\u001b[4l");     // IRM off (RM 4)
        write(terminal, "\u001b[1G");     // x=0
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
        write(terminal, "\u001b[1;1H");
        write(terminal, "ABCDEFGH");
        write(terminal, "\u001b[1;1H");
        resetDirty();
        write(terminal, "\u001b[2X"); // ECH 2: erase 2 chars at the cursor, no shift
        // The erased buffer row renders at screen row scrollBack, not at row 0 (terminal.y).
        assertEquals(1 << scrollBack, renderer.dirtyMask.get() & 0xFFFFFF,
            "ECH while scrolled back must mark the screen row where the cursor row renders, not terminal.y");
    }

    @Test
    void decscnmToggleMarksWholeScreenDirty() {
        assertFalse(terminal.currentPrivateModeState.DECSCNM);
        resetDirty();
        write(terminal, "\u001b[?5h");    // DECSCNM on
        assertTrue(terminal.currentPrivateModeState.DECSCNM, "?5h enables screen-inverse");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF, "DECSCNM toggle must redraw the whole screen");
        resetDirty();
        write(terminal, "\u001b[?5l");    // DECSCNM off
        assertFalse(terminal.currentPrivateModeState.DECSCNM);
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF, "DECSCNM toggle must redraw the whole screen");
    }

    @Test
    void xtrestoreDecscnmRestoresAndMarksWholeScreenDirty() {
        write(terminal, "\u001b[?5h");    // DECSCNM on
        write(terminal, "\u001b[?5s");    // XTSAVE mode 5: save DECSCNM=true
        assertTrue(terminal.savePrivateModeState.DECSCNM, "XTSAVE must capture DECSCNM");
        write(terminal, "\u001b[?5l");    // DECSCNM off
        assertFalse(terminal.currentPrivateModeState.DECSCNM);
        resetDirty();
        write(terminal, "\u001b[?5r");    // XTRESTORE mode 5: restore DECSCNM=true
        assertTrue(terminal.currentPrivateModeState.DECSCNM, "XTRESTORE must restore DECSCNM");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get() & 0xFFFFFF,
            "restoring DECSCNM via XTRESTORE must redraw the whole screen, like DECSET/DECRST");
    }

    @Test
    void echErasedCellsTakeDefaultForegroundAndCurrentBackground() {
        write(terminal, "\u001b[41m");    // bg = SIXTEEN_COLOR red (sixteenColor.G = 1)
        write(terminal, "ABCDEFGH");
        write(terminal, "\u001b[3G");     // x=2
        write(terminal, "\u001b[2X");     // ECH 2
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

    private void write(final Terminal target, final String text) {
        target.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    private void fillRows(final int startRow, final String chars) {
        for (int i = 0; i < chars.length(); i++) {
            write(terminal, "\u001b[" + (startRow + 1 + i) + ";1H" + chars.charAt(i));
        }
    }

    private char charAt(final int x, final int y) {
        final int row = y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
        return (char) terminal.buffer[x + row * Terminal.WIDTH];
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
        write(terminal, "\u001b[1T"); // scroll down 1 line
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
        write(terminal, "\u001b[2;8r"); // scroll region rows 1..7 (0-indexed)
        resetDirty();
        write(terminal, "\u001b[1S");  // scroll up 1 within the margin
        int expected = 0;
        for (int i = 1; i <= 7; i++) {
            expected |= (1 << i);
        }
        assertEquals(expected, renderer.dirtyMask.get() & 0xFF,
            "Margin scroll after scrollback growth must mark the margin rows, not stale buffer rows");
    }
}