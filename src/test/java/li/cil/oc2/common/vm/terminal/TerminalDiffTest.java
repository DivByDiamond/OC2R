package li.cil.oc2.common.vm.terminal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import li.cil.oc2.common.vm.terminal.color.TerminalColors.ColorData;
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
        assertNotEquals(0, client.renderers.size() >= 0); // sanity: no throw
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

    private static void write(final Terminal target, final String text) {
        target.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static char charAt(final Terminal terminal, final int x, final int y) {
        return (char) terminal.buffer[cellIndex(terminal, x, y)];
    }

    private static int cellIndex(final Terminal terminal, final int x, final int y) {
        final int row = y + terminal.lastRowToDisplayMax - Terminal.HEIGHT;
        return x + row * terminal.width;
    }
}
