package li.cil.oc2.common.vm.terminal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire round-trip tests for {@link TerminalDiff#STREAM_CODEC}. Regression coverage for the
 * palette-sync byte-order desync: the palette was written last (after {@code inputModes}) but
 * read right after {@code rowData}, so every snapshot decoded from the wrong offset — in-game
 * this corrupted all diff traffic while the capture-to-apply tests (which skip the codec)
 * stayed green. Both palette branches must round-trip byte-exactly.
 */
public class TerminalDiffCodecTest {
    private static final String ESC = String.valueOf((char) 27);
    private static final String BEL = String.valueOf((char) 7);

    @Test
    void codecRoundTripWithPalettePreservesAllFields() {
        final Terminal server = new Terminal();
        write(server, ESC + "]4;16;rgb:ff/00/00" + BEL);
        write(server, "hello");
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);

        final TerminalDiff.Snapshot decoded = roundTrip(snapshot);

        assertEquals(snapshot.reset(), decoded.reset(), "reset");
        assertEquals(snapshot.width(), decoded.width(), "width");
        assertEquals(snapshot.height(), decoded.height(), "height");
        assertEquals(snapshot.altBuffer(), decoded.altBuffer(), "altBuffer");
        assertArrayEquals(snapshot.rows(), decoded.rows(), "rows");
        assertEquals(snapshot.cursorX(), decoded.cursorX(), "cursorX");
        assertEquals(snapshot.cursorY(), decoded.cursorY(), "cursorY");
        assertEquals(snapshot.lastRowToDisplay(), decoded.lastRowToDisplay(), "lastRowToDisplay");
        assertEquals(snapshot.lastRowToDisplayMax(), decoded.lastRowToDisplayMax(),
                "lastRowToDisplayMax");
        assertEquals(snapshot.cursorMode(), decoded.cursorMode(), "cursorMode");
        assertEquals(snapshot.cursorVisible(), decoded.cursorVisible(), "cursorVisible");
        assertEquals(snapshot.bell(), decoded.bell(), "bell");
        assertEquals(snapshot.inputModes(), decoded.inputModes(), "inputModes");
        assertArrayEquals(snapshot.palette(), decoded.palette(),
                "palette must survive the wire (was decoded empty-length from cursorX's byte)");
    }

    @Test
    void codecRoundTripWithoutPaletteConsumesAllBytes() {
        final Terminal server = new Terminal();
        write(server, "steady state");
        TerminalDiff.capture(server); // ship the initial palette (revision bump from RIS)
        write(server, "more");
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);

        final ByteBuf buf = Unpooled.buffer();
        TerminalDiff.STREAM_CODEC.encode(buf, snapshot);
        final TerminalDiff.Snapshot decoded = TerminalDiff.STREAM_CODEC.decode(buf);

        assertNull(decoded.palette(), "unchanged palette must stay absent on the wire");
        assertEquals(snapshot.cursorX(), decoded.cursorX(), "cursorX");
        assertEquals(snapshot.inputModes(), decoded.inputModes(), "inputModes");
        assertEquals(0, buf.readableBytes(), "read and write order must agree to the last byte");
    }

    @Test
    void codecRoundTripPreservesHeightField() {
        final Terminal server = new Terminal();
        write(server, "hello");
        TerminalDiff.capture(server); // drain initial
        server.height = 48; // simulate a DECSLPP resize
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);

        final TerminalDiff.Snapshot decoded = roundTrip(snapshot);

        assertEquals(48, decoded.height(), "height must survive the wire");
    }

    @Test
    void fullRefreshSnapshotCarriesPaletteForLateJoiners() {
        // Late-joiner scenario: the palette change was already shipped to an earlier client
        // (revision == lastSent), then a new client needs a full rebuild. The reset-flagged
        // full-refresh captures row content again — the palette must ride it too, or the
        // late joiner renders stale defaults until the next OSC 4/104/RIS.
        final Terminal server = new Terminal();
        write(server, ESC + "]4;16;rgb:ff/00/00" + BEL);
        TerminalDiff.capture(server); // shipped to the first client; lastSent == revision

        server.markAllDirty(); // forces networkNeedsFullRefresh -> capture emits reset=true
        final TerminalDiff.Snapshot full = TerminalDiff.capture(server);
        assertTrue(full.reset(),
                "precondition: this is a full-refresh (reset) snapshot");
        assertNotNull(full.palette(),
                "a reset snapshot must carry the palette even when the revision is unchanged");
    }

    @Test
    void codecRoundTripPreservesShiftOps() {
        // At absolute capacity a linefeed physically shifts the whole main buffer; the shift
        // is recorded as resolved memmove geometry and must survive the wire byte-exactly —
        // the client replays it to keep its scrollback copy in sync.
        final Terminal server = new Terminal();
        write(server, "\n".repeat(Terminal.HEIGHT * Terminal.SCROLL_BACK_COUNT));
        TerminalDiff.capture(server); // drain: the saturation's own shifts
        write(server, "\n"); // one more line at capacity -> exactly one new shift op
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);

        assertEquals(1, snapshot.shiftOps().length / 5, "precondition: one shift op recorded");
        assertNotEquals(0, snapshot.shiftOps()[2], "the op shifts a nonzero number of rows");

        final TerminalDiff.Snapshot decoded = roundTrip(snapshot);
        assertArrayEquals(snapshot.shiftOps(), decoded.shiftOps(),
                "shift op geometry must survive the wire");
    }

    @Test
    void hostileRowCountIsBoundedAndConsumesTheStream() {
        // A malformed rowCount (larger than the rows array) must not become an unbounded
        // allocation; extra entries are consumed (stream integrity) and dropped. The reverse
        // (rowCount < rows) decodes nulls, which apply skips.
        final Terminal server = new Terminal();
        write(server, "hello");
        final TerminalDiff.Snapshot snapshot = TerminalDiff.capture(server);

        final TerminalDiff.Snapshot hostile =
                new TerminalDiff.Snapshot(
                        snapshot.reset(),
                        snapshot.width(),
                        snapshot.height(),
                        snapshot.altBuffer(),
                        snapshot.rows(), // 1 row index...
                        new byte[][] {snapshot.rowData()[0], snapshot.rowData()[0], snapshot.rowData()[0]},
                        snapshot.shiftOps(),
                        snapshot.cursorX(),
                        snapshot.cursorY(),
                        snapshot.lastRowToDisplay(),
                        snapshot.lastRowToDisplayMax(),
                        snapshot.cursorMode(),
                        snapshot.cursorVisible(),
                        snapshot.bell(),
                        snapshot.inputModes(),
                        snapshot.palette());

        final TerminalDiff.Snapshot decoded = roundTrip(hostile);
        assertEquals(1, decoded.rowData().length, "rowData bounded to the rows array length");
        assertNotNull(decoded.rowData()[0], "the paired row payload survives");

        final Terminal client = new Terminal();
        assertDoesNotThrow(() -> TerminalDiff.apply(client, decoded));
    }

    private static TerminalDiff.Snapshot roundTrip(final TerminalDiff.Snapshot snapshot) {
        final ByteBuf buf = Unpooled.buffer();
        TerminalDiff.STREAM_CODEC.encode(buf, snapshot);
        final TerminalDiff.Snapshot decoded = TerminalDiff.STREAM_CODEC.decode(buf);
        assertEquals(0, buf.readableBytes(), "all bytes consumed");
        return decoded;
    }

    private static void write(final Terminal target, final String text) {
        target.io.putOutput(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }
}
