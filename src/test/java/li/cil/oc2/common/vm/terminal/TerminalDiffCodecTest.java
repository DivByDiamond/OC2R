package li.cil.oc2.common.vm.terminal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
