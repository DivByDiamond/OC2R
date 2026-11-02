package li.cil.oc2.common.vm.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Row-aware cursor, wrap, tab and edit behaviour of double-width/height rows (section 44.3). */
class DoubleSizeLineTest {
    private static final int HALF = Terminal.WIDTH / 2;
    private Terminal terminal;

    @BeforeEach
    void setUp() {
        terminal = new Terminal();
    }

    @Test
    void doubledRowExposesHalfTheColumns() {
        assertEquals(Terminal.WIDTH - 1, terminal.lastColumn(0));
        write("\033#6");
        assertEquals(Terminal.LINE_ATTR_DOUBLE_WIDTH, terminal.lineAttrOfScreenRow(0));
        assertEquals(HALF - 1, terminal.lastColumn(0));
        assertEquals(Terminal.WIDTH - 1, terminal.lastColumn(1));
    }

    @Test
    void cursorPositioningStopsAtTheVisibleHalf() {
        write("\033#3");
        terminal.setCursorPos(Terminal.WIDTH - 1, 0);
        assertEquals(HALF - 1, terminal.x);
        write("\033[1;70H");
        assertEquals(HALF - 1, terminal.x);
    }

    @Test
    void printingWrapsAtTheVisibleHalf() {
        write("\033#6");
        write("x".repeat(HALF));
        assertEquals(HALF - 1, terminal.x);
        assertEquals(0, terminal.y);
        write("y");
        assertEquals(1, terminal.y);
        assertEquals(1, terminal.x);
        assertEquals('y', charAt(0, 1));
        assertEquals(' ', charAt(HALF, 0), "nothing is written into the hidden half");
    }

    @Test
    void tabStopsAtTheVisibleHalf() {
        write("\033#6");
        write("\t".repeat(20));
        assertEquals(HALF - 1, terminal.x);
        write("\r\033[100I");
        assertEquals(HALF - 1, terminal.x);
    }

    @Test
    void convertingARowDiscardsTheRightHalfAndClampsTheCursor() {
        write("a".repeat(Terminal.WIDTH));
        assertEquals(Terminal.WIDTH - 1, terminal.x);
        write("\033#6");
        assertEquals('a', charAt(HALF - 1, 0));
        assertEquals(' ', charAt(HALF, 0));
        assertEquals(' ', charAt(Terminal.WIDTH - 1, 0));
        assertEquals(HALF - 1, terminal.x);
        assertFalse(terminal.autowrapPending, "a clamped cursor cannot have a pending wrap");

        write("\033#5");
        assertEquals(' ', charAt(HALF, 0), "returning to single width must not resurrect the text");
    }

    @Test
    void insertDoesNotPushTextIntoTheHiddenHalf() {
        write("\033#6");
        write("abc");
        write("\033[1;1H\033[100@");
        assertEquals(' ', charAt(0, 0));
        assertEquals(' ', charAt(HALF, 0));
        write("\033#5");
        assertEquals(' ', charAt(HALF, 0));
    }

    @Test
    void lineSizeChangeIsIgnoredWhileLeftRightMarginsAreEnabled() {
        write("\033[?69h");
        write("\033#6");
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(0));
    }

    @Test
    void movingDownIntoADoubledRowClampsTheColumn() {
        write("\033[2;1H\033#6");
        write("\033[1;70H");
        assertEquals(69, terminal.x);
        write("\033[B");
        assertEquals(1, terminal.y);
        assertEquals(HALF - 1, terminal.x);
    }

    private void write(final String s) {
        terminal.io.putOutput(ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)));
    }

    private int charAt(final int x, final int y) {
        return terminal.buffer[x + (y + terminal.lastRowToDisplayMax - terminal.height) * terminal.width];
    }
}
