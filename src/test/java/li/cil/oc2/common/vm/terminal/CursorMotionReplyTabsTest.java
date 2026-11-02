package li.cil.oc2.common.vm.terminal;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CursorMotionReplyTabsTest {
    private final Terminal terminal = new Terminal();

    private void write(final String value) {
        terminal.io.putOutput(ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "B", "C", "D"})
    void cardinalMovesDefaultZeroAndLargeCounts(final String command) {
        final int dx = "C".equals(command) ? 1 : "D".equals(command) ? -1 : 0;
        final int dy = "B".equals(command) ? 1 : "A".equals(command) ? -1 : 0;
        write("\033[10;20H\033[" + command + "\033[0" + command);
        assertEquals(19 + 2 * dx, terminal.x);
        assertEquals(9 + 2 * dy, terminal.y);
        write("\033[999999999999999999999" + command);
        assertEquals(dx == 0 ? 19 : dx > 0 ? 79 : 0, terminal.x);
        assertEquals(dy == 0 ? 9 : dy > 0 ? 23 : 0, terminal.y);
    }

    @Test
    void verticalMovesRespectRegionWithoutScrolling() {
        write("TOP\033[3;8r\033[5;1H\033[999B");
        assertEquals(7, terminal.y);
        write("\033[999A");
        assertEquals(2, terminal.y);
        assertEquals('T', terminal.buffer[(terminal.lastRowToDisplayMax - terminal.height) * terminal.width]);
    }

    @Test
    void statusAndDeviceAttributeRepliesAreExactAndQueuedInOrder() {
        write("\033[n\033[5n\033[?5n\033[c\033[>c");
        assertEquals("\033[0n\033[0n\033[?0n\033[?1;2c\033[>61;10;20993c", input());
        assertNull(terminal.io.getInput());
    }

    @Test
    void cursorRepliesUseOriginRelativeCoordinates() {
        write("\033[3;10r\033[?69h\033[5;30s\033[?6h\033[2;3H\033[6n\033[?6n");
        assertEquals("\033[2;3R\033[?2;3;1R", input());
        write("\033[?6l\033[4;7H\033[6n");
        assertEquals("\033[4;7R", input());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\033[?47h"})
    void customTabsClearAndRestoreOnColumnMode(final String buffer) {
        write(buffer + "\t");
        assertEquals(8, terminal.x);
        write("\033[3g\033[1;6H\033H\r\t");
        assertEquals(5, terminal.x);
        write("\033[g\r\033[I");
        assertEquals(79, terminal.x);
        write("\033[?3h\r\033[20I");
        assertEquals(131, terminal.x);
        write("\r\t");
        assertEquals(8, terminal.x);
        write("\033[?3l\r\t");
        assertEquals(8, terminal.x);
    }

    @Test
    void customStopsAndCurrentStopClearStayLocalToBuffer() {
        write("\033[1;6H\033H\033[1;9H\033[g\033[1;7H\033[Z");
        assertEquals(5, terminal.x);
        write("\033[?47h\r\t");
        assertEquals(8, terminal.x);
        write("\033[?47l\r\t");
        assertEquals(5, terminal.x);
    }

    @Test
    void cursorReplyAtPendingWrapStaysInsideLastVisibleColumn() {
        write("\033#6" + "x".repeat(40) + "\033[6n");
        assertEquals("\033[1;40R", input());
    }

    private String input() {
        final ByteBuffer bytes = terminal.io.getInput();
        if (bytes == null) return "";
        final byte[] data = new byte[bytes.remaining()];
        bytes.get(data);
        return new String(data, StandardCharsets.US_ASCII);
    }
}
