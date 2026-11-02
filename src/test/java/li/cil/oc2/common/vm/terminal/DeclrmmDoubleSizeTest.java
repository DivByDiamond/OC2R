package li.cil.oc2.common.vm.terminal;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DeclrmmDoubleSizeTest {
    private final Terminal terminal = new Terminal();

    private void write(final String value) {
        terminal.io.putOutput(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\033[?47h"})
    void enablingMarginsNormalizesExistingDoubledRowsWithoutErasing(final String buffer) {
        write(buffer + "\033#3TOP\033[2;1H\033#4BOTTOM\033[3;1H\033#6WIDE\033[?69h");
        for (int row = 0; row < 3; row++) assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(row));
        assertEquals(79, terminal.lastColumn());
        write("\033#6");
        assertEquals(Terminal.LINE_ATTR_SINGLE, terminal.lineAttrOfScreenRow(2));
        write("\033[5;30s\033[?69l");
        assertEquals(0, terminal.scrollColFirst);
        assertEquals(79, terminal.scrollColLast);
        write("\033[3;1H\033#6");
        assertEquals(Terminal.LINE_ATTR_DOUBLE_WIDTH, terminal.lineAttrOfScreenRow(2));
        final int[] cells = terminal.currentPrivateModeState.isAltBufferEnabled() ? terminal.altBuffer : terminal.buffer;
        final int first = terminal.currentPrivateModeState.isAltBufferEnabled() ? 0 : terminal.lastRowToDisplayMax - terminal.height;
        assertEquals('T', cells[first * terminal.width]);
        assertEquals('B', cells[(first + 1) * terminal.width]);
    }
}
