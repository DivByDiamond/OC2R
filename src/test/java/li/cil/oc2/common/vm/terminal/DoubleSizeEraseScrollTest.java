package li.cil.oc2.common.vm.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Erase and scroll semantics on doubled rows, in both terminal buffers. */
class DoubleSizeEraseScrollTest {
    private static final int HALF = Terminal.WIDTH / 2;
    private final Terminal terminal = new Terminal();

    @ParameterizedTest
    @ValueSource(strings = {"", "\033[?1049h"})
    void eraseCharactersAndLinesPreserveScaleAndNeighbours(final String buffer) {
        write(buffer + "\033#6" + "a".repeat(HALF) + "\033[2;1HNEIGHBOUR");
        write("\033[1;3H\033[2X");
        assertEquals('a', activeChar(1, 0));
        assertEquals(' ', activeChar(2, 0));
        assertEquals(' ', activeChar(3, 0));
        assertEquals('a', activeChar(4, 0));
        write("\033[1K");
        assertEquals(' ', activeChar(0, 0));
        assertEquals('a', activeChar(4, 0));
        write("\033[K");
        assertEquals(' ', activeChar(HALF - 1, 0));
        assertEquals('N', activeChar(0, 1));
        assertEquals(Terminal.LINE_ATTR_DOUBLE_WIDTH, terminal.lineAttrOfScreenRow(0));
        assertEquals(2, terminal.x);
        write("\033[1;1H" + "b".repeat(HALF) + "\033[1;40H\033[999X");
        assertEquals('b', activeChar(HALF - 2, 0));
        assertEquals(' ', activeChar(HALF - 1, 0));
        assertEquals('N', activeChar(0, 1));
        write("\033[1;1H" + "c".repeat(HALF) + "\033[1;3H\033[2K");
        assertEquals(Terminal.LINE_ATTR_DOUBLE_WIDTH, terminal.lineAttrOfScreenRow(0));
        assertEquals('N', activeChar(0, 1));
        write("\033#5");
        for (int x = 0; x < terminal.width; x++) assertEquals(' ', activeChar(x, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\033[?1049h"})
    void displayEraseIgnoresScrollMarginsAndPreservesPartialRowAttributes(final String buffer) {
        write(buffer + "\033#3TOP\033[2;1H\033#4BOTTOM\033[3;1H\033#6WIDE"
                + "\033[4;1HOUTSIDE\033[2;3r\033[2;3H\033[J");
        assertEquals('T', activeChar(0, 0));
        assertEquals('B', activeChar(0, 1));
        assertEquals(' ', activeChar(2, 1));
        assertEquals(' ', activeChar(0, 3));
        assertEquals(Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM, terminal.lineAttrOfScreenRow(1));
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(2));
        write("\033[1J");
        assertEquals(' ', activeChar(0, 0));
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(0));
        assertEquals(Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM, terminal.lineAttrOfScreenRow(1));
        write("\033[1;1H\033#3REFILLED\033[2;1H\033#4REFILLED\033[2J");
        for (int y = 0; y < terminal.height; y++) {
            assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(y));
            for (int x = 0; x < terminal.width; x++) assertEquals(' ', activeChar(x, y));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\033[?1049h"})
    void scrollRegionMovesAttributesAndBlanksExposedRows(final String buffer) {
        write(buffer + "\033#6OUTSIDE\033[2;1H\033#3TOP\033[3;1H\033#4BOTTOM"
                + "\033[4;1H\033#6WIDE\033[5;1HKEEP\033[2;4r\033[4;1H\n");
        assertEquals(Terminal.LINE_ATTR_DOUBLE_WIDTH, terminal.lineAttrOfScreenRow(0));
        assertEquals('O', activeChar(0, 0));
        assertEquals(Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM, terminal.lineAttrOfScreenRow(1));
        assertEquals('B', activeChar(0, 1));
        assertEquals(Terminal.LINE_ATTR_DOUBLE_WIDTH, terminal.lineAttrOfScreenRow(2));
        assertEquals('W', activeChar(0, 2));
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(3));
        assertEquals(' ', activeChar(0, 3));
        assertEquals('K', activeChar(0, 4));
        write("\033[2;1H\033M");
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(1));
        assertEquals(Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM, terminal.lineAttrOfScreenRow(2));
        assertEquals(Terminal.LINE_ATTR_DOUBLE_WIDTH, terminal.lineAttrOfScreenRow(3));
        assertEquals('K', activeChar(0, 4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\033[?1049h"})
    void displayEraseResetsFullyErasedCursorRowScale(final String buffer) {
        write(buffer + "\033#6WIDE\033[1;1H\033[J");
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(0));
        write("\033#3TOP\033[1;40H\033[1J");
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(0));
        assertEquals(39, terminal.x);
        assertEquals(' ', activeChar(0, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"S", "T", "L", "M", "altS", "altT", "altL", "altM"})
    void explicitScrollAndLineEditsMoveScaleWithinMargins(final String operation) {
        final boolean alt = operation.startsWith("alt");
        final char command = operation.charAt(operation.length() - 1);
        write((alt ? "\033[?1049h" : "") + "\033#6KEEP\033[2;1H\033#3TOP"
                + "\033[3;1H\033#4BOTTOM\033[4;1H\033#6WIDE\033[5;1HOUTSIDE"
                + "\033[2;4r\033[2;1H\033[1" + command);
        final boolean up = command == 'S' || command == 'M';
        assertEquals(Terminal.LINE_ATTR_DOUBLE_WIDTH, terminal.lineAttrOfScreenRow(0));
        assertEquals('K', activeChar(0, 0));
        assertEquals('O', activeChar(0, 4));
        if (up) {
            assertRow(1, Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM, 'B');
            assertRow(2, Terminal.LINE_ATTR_DOUBLE_WIDTH, 'W');
            assertRow(3, Terminal.LINE_ATTR_SINGLE, ' ');
        } else {
            assertRow(1, Terminal.LINE_ATTR_SINGLE, ' ');
            assertRow(2, Terminal.LINE_ATTR_DOUBLE_HEIGHT_TOP, 'T');
            assertRow(3, Terminal.LINE_ATTR_DOUBLE_HEIGHT_BOTTOM, 'B');
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\033[?1049h"})
    void displayEraseFillsNewlyExposedHalfWithEraseBackground(final String buffer) {
        write(buffer + "\033#6WIDE\033[48;2;12;34;56m\033[1;40H\033[1J");
        final boolean alt = terminal.currentPrivateModeState.isAltBufferEnabled();
        final int row = alt ? 0 : terminal.lastRowToDisplayMax - terminal.height;
        final var backgrounds = alt ? terminal.altColorsBackground : terminal.colorsBackground;
        assertEquals(0x0C2238, backgrounds[row * terminal.width].toInt());
        assertEquals(0x0C2238, backgrounds[row * terminal.width + terminal.width - 1].toInt());
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(0));
    }

    private void assertRow(final int row, final byte attribute, final char firstCharacter) {
        assertEquals(attribute, terminal.lineAttrOfScreenRow(row));
        assertEquals(firstCharacter, activeChar(0, row));
    }

    private int activeChar(final int x, final int y) {
        return terminal.currentPrivateModeState.isAltBufferEnabled()
                ? terminal.altBuffer[y * terminal.width + x] : charAt(x, y);
    }

    private void write(final String s) {
        terminal.io.putOutput(ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)));
    }

    private int charAt(final int x, final int y) {
        return terminal.buffer[x + (y + terminal.lastRowToDisplayMax - terminal.height) * terminal.width];
    }
}
