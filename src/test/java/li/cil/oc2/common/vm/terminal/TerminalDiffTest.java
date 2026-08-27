package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round-trip tests for the server->client terminal screen diff (TerminalDiff). */
public class TerminalDiffTest {
    private static final String ESC = "\u001b";
    private static final String CSI = ESC + "[";

    private Terminal server;

    @BeforeEach
    void setUp() {
        server = new Terminal();
    }

    @Test
    void captureThenApplyReproducesCellsAndCursor() {
        write(server, CSI + "2;1H" + CSI + "31mRED" + CSI + "0m" + CSI + "4;5H");

        final Terminal client = new Terminal();
        TerminalDiff.apply(client, TerminalDiff.capture(server));

        assertEquals('R', charAt(client, 0, 1));
        assertEquals('E', charAt(client, 1, 1));
        assertEquals('D', charAt(client, 2, 1));
        assertEquals(' ', charAt(client, 0, 0));
        // Cursor was parked at 4;5 by CUP (x=4, y=3).
        assertEquals(4, client.x);
        assertEquals(3, client.y);
        // Foreground of the first cell resolved as palette index 1 (red) in 16-color mode.
        final ColorData fg = client.colors[cellIndex(client, 0, 1)];
        assertEquals(1, fg.R);
    }

    @Test
    void incrementalDiffOnlyCarriesChangedRows() {
        write(server, CSI + "2;1HABC");
        final int[] first = TerminalDiff.capture(server).rows();
        assertEquals(1, first.length);
        // Screen row 1 at the bottom of the scrollback window == absolute buffer row 1.
        assertEquals(1, first[0]);

        // A cursor-only move still produces a snapshot (cursor travels along), but the
        // next real output after an idle period must re-send the touched row.
        write(server, CSI + "5;1Hxyz");
        final TerminalDiff.Snapshot second = TerminalDiff.capture(server);
        assertTrue(second.rows().length >= 1);
    }

    @Test
    void resetSnapshotClearsClientState() {
        write(server, CSI + "2;1HSTALE");
        final Terminal client = new Terminal();
        TerminalDiff.apply(client, TerminalDiff.capture(server));
        assertEquals('S', charAt(client, 0, 1));

        // Server resets (VM restart): RIS + full snapshot.
        li.cil.oc2.common.vm.terminal.escapes.index.RIS.execute(server);
        final TerminalDiff.Snapshot reset = TerminalDiff.captureFull(server);
        assertTrue(reset.reset());
        TerminalDiff.apply(client, reset);
        assertNotEquals('S', charAt(client, 0, 1));
        assertEquals(' ', charAt(client, 0, 1));
    }

    @Test
    void inputModeFlagsRoundTrip() {
        write(server, CSI + "?1002h" + CSI + "?1006h"); // cell-motion + SGR mouse
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);

        final Terminal client = new Terminal();
        TerminalDiff.apply(client, snapshot);
        assertTrue(client.currentPrivateModeState.CELL_MOTION_MOUSE);
        assertTrue(client.currentPrivateModeState.SGR_MOUSE);
        assertFalse(client.currentPrivateModeState.DECCKM);
    }

    @Test
    void bellFlagIsConsumedByCapture() {
        write(server, "\u0007");
        assertTrue(TerminalDiff.capture(server).bell());

        // The flag must not leak into every subsequent diff (issue #23).
        write(server, "a");
        assertFalse(TerminalDiff.capture(server).bell());
        write(server, "b");
        assertFalse(TerminalDiff.capture(server).bell());

        // A new bell is delivered exactly once.
        write(server, "\u0007");
        assertTrue(TerminalDiff.capture(server).bell());
        assertFalse(TerminalDiff.capture(server).bell());
    }

    @Test
    void rowSerializationRoundTripsBitExact() {
        // Poke cells directly to cover values the escape parser cannot produce:
        // max Unicode codepoint, negative style byte, explicit truecolor channels.
        server.buffer[0] = 0x10FFFF;
        server.colors[0] = new ColorData(255, 254, 253, ColorMode.TRUE_COLOR);
        server.colorsBackground[0] = new ColorData(1, 2, 3, ColorMode.TWO_FIFTY_SIX_COLOR);
        server.styles[0] = (byte) 0xFF;
        server.buffer[1] = 'A';
        server.styles[1] = (byte) -1;
        server.buffer[2] = 'é';
        server.colors[2] = new ColorData(7, 0, 0, ColorMode.SIXTEEN_COLOR_BRIGHT);
        server.buffer[server.width + 5] = 'Z';

        final TerminalDiff.Snapshot snapshot = TerminalDiff.captureFull(server);
        final Terminal client = new Terminal();
        TerminalDiff.apply(client, snapshot);

        assertRowsEqual(server, client, snapshot.rows(), false);
    }

    @Test
    void altBufferRowsRoundTripBitExact() {
        write(server, CSI + "?1049h" + CSI + "2;1H" + CSI + "38;2;10;20;30m" + CSI + "1;4m" + "héllo→");
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);
        assertTrue(snapshot.altBuffer());

        final Terminal client = new Terminal();
        TerminalDiff.apply(client, snapshot);

        assertRowsEqual(server, client, snapshot.rows(), true);
    }

    @Test
    void repeatedCellsAreSpanCompressed() {
        write(server, CSI + "2;1H" + "A".repeat(80));
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);
        assertEquals(1, snapshot.rowData().length);
        // One run header varint plus one cell: far below one-payload-per-cell.
        assertTrue(snapshot.rowData()[0].length <= 6);
    }

    @Test
    void singleCharacterEchoIsCompact() {
        write(server, CSI + "2;1HX");
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);
        assertEquals(1, snapshot.rowData().length);
        // The old fixed-width format cost 80 * 37 = 2960 bytes for this row.
        assertTrue(snapshot.rowData()[0].length < 40);
    }

    @Test
    void truncatedRowPayloadDoesNotThrow() {
        write(server, CSI + "2;1Hhello");
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);
        final byte[][] truncated = {Arrays.copyOf(snapshot.rowData()[0], 4)};
        final TerminalDiff.Snapshot broken =
                new TerminalDiff.Snapshot(
                        snapshot.reset(),
                        snapshot.width(),
                        snapshot.altBuffer(),
                        snapshot.rows(),
                        truncated,
                        snapshot.cursorX(),
                        snapshot.cursorY(),
                        snapshot.lastRowToDisplay(),
                        snapshot.lastRowToDisplayMax(),
                        snapshot.cursorMode(),
                        snapshot.cursorVisible(),
                        snapshot.bell(),
                        snapshot.inputModes());
        final Terminal client = new Terminal();
        TerminalDiff.apply(client, broken); // must not throw
    }

    private static void write(final Terminal target, final String text) {
        target.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    private static void assertRowsEqual(
            final Terminal expected, final Terminal actual, final int[] rows, final boolean alt) {
        for (final int row : rows) {
            for (int x = 0; x < expected.width; x++) {
                final int column = x;
                final int index = row * expected.width + x;
                assertEquals(
                        alt ? expected.altBuffer[index] : expected.buffer[index],
                        alt ? actual.altBuffer[index] : actual.buffer[index],
                        () -> "codepoint mismatch at row " + row + ", column " + column);
                final ColorData expectedFg = alt ? expected.altColors[index] : expected.colors[index];
                final ColorData actualFg = alt ? actual.altColors[index] : actual.colors[index];
                assertEquals(expectedFg.toInt(), actualFg.toInt());
                assertEquals(expectedFg.Mode, actualFg.Mode);
                final ColorData expectedBg =
                        alt ? expected.altColorsBackground[index] : expected.colorsBackground[index];
                final ColorData actualBg =
                        alt ? actual.altColorsBackground[index] : actual.colorsBackground[index];
                assertEquals(expectedBg.toInt(), actualBg.toInt());
                assertEquals(expectedBg.Mode, actualBg.Mode);
                assertEquals(
                        alt ? expected.altStyles[index] : expected.styles[index],
                        alt ? actual.altStyles[index] : actual.styles[index]);
            }
        }
    }

    private static char charAt(final Terminal terminal, final int x, final int y) {
        return (char) terminal.buffer[cellIndex(terminal, x, y)];
    }

    private static int cellIndex(final Terminal terminal, final int x, final int y) {
        final int row = y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
        return x + row * terminal.width;
    }
}
