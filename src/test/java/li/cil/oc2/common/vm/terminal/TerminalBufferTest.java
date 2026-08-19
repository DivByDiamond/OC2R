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
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode);
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
        assertEquals(0xFFFFFF, renderer.dirtyMask.get());
        resetDirty();
        write(terminal, "\u001b[?47l");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get());
        resetDirty();
        write(terminal, "\u001b[?1049h");
        assertEquals(0xFFFFFF, renderer.dirtyMask.get());
    }

    @Test
    void sgr256ColorForeground() {
        write(terminal, "\u001b[38;5;196mX");
        assertEquals(TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR, terminal.currentForegroundColorMode);
        assertEquals(196, terminal.twoFiftySixColor.R);
        assertEquals('X', charAt(0, 0));
        assertEquals(TerminalColors.ColorMode.TWO_FIFTY_SIX_COLOR, terminal.colors[0].Mode);
        assertEquals(196, terminal.colors[0].R);
    }

    @Test
    void sgrTrueColorForegroundAndBackground() {
        write(terminal, "\u001b[38;2;100;150;200;48;2;10;20;30mY");
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.currentForegroundColorMode);
        assertEquals(100, terminal.foregroundColor.R);
        assertEquals(150, terminal.foregroundColor.G);
        assertEquals(200, terminal.foregroundColor.B);
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.currentBackgroundColorMode);
        assertEquals(10, terminal.backgroundColor.R);
        assertEquals(20, terminal.backgroundColor.G);
        assertEquals(30, terminal.backgroundColor.B);
        assertEquals('Y', charAt(0, 0));
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.colors[0].Mode);
        assertEquals(100, terminal.colors[0].R);
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.colorsBackground[0].Mode);
        assertEquals(10, terminal.colorsBackground[0].R);
    }

    @Test
    void sgrTrueColorKeepsFollowingAttributes() {
        write(terminal, "\u001b[38;2;100;150;200;1mZ");
        assertEquals(TerminalColors.ColorMode.TRUE_COLOR, terminal.currentForegroundColorMode);
        assertEquals(100, terminal.foregroundColor.R);
        assertEquals(Terminal.STYLE_BOLD_MASK, terminal.style & Terminal.STYLE_BOLD_MASK);
        assertEquals(Terminal.STYLE_BOLD_MASK, terminal.styles[0] & Terminal.STYLE_BOLD_MASK);
    }

    @Test
    void sgrResetRestoresDefaults() {
        write(terminal, "\u001b[38;2;100;150;200;48;5;52;1mX");
        write(terminal, "\u001b[0mY");
        assertEquals(TerminalColors.ColorMode.SIXTEEN_COLOR, terminal.currentForegroundColorMode);
        assertEquals(TerminalColors.ColorMode.DEFAULT_BACKGROUND, terminal.currentBackgroundColorMode);
        assertEquals(TerminalColors.DEFAULT_STYLE, terminal.style);
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
        assertEquals(0xFFFFFF, renderer.dirtyMask.get());
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
}
