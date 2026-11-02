package li.cil.oc2.common.vm.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import li.cil.oc2.common.vm.terminal.buffer.utf8.Utf8Decoder;
import org.junit.jupiter.api.Test;

class Utf8DecoderTest {
    @Test
    void decodesAllSequenceLengthsAndConsecutiveScalars() {
        assertEquals("Aé中😀Z", decode("Aé中😀Z".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void incompleteSequenceSurvivesIndividualCalls() {
        final Utf8Decoder decoder = new Utf8Decoder();
        assertFalse(decoder.process((byte) 0xF0));
        assertFalse(decoder.process((byte) 0x9F));
        assertFalse(decoder.process((byte) 0x98));
        assertTrue(decoder.process((byte) 0x80));
        assertEquals(0x1F600, decoder.getCodepoint());
    }

    @Test
    void invalidSequencesAreDiscardedAndResynchronize() {
        assertEquals("AéB", decode(new byte[] {(byte) 0x80, (byte) 0xFF, 'A', (byte) 0xE2,
                (byte) 0xC3, (byte) 0xA9, 'B'}));
        assertEquals("ABC", decode(new byte[] {(byte) 0xC0, (byte) 0xAF, 'A',
                (byte) 0xED, (byte) 0xA0, (byte) 0x80, 'B',
                (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80, 'C'}));
    }

    @Test
    void terminalReplaysMultibyteScalarsOneByteAtATime() {
        final Terminal terminal = new Terminal();
        final ByteBuffer chunk = ByteBuffer.allocate(1);
        for (final byte value : "é中😀".getBytes(StandardCharsets.UTF_8)) {
            chunk.clear();
            chunk.put(value);
            chunk.flip();
            terminal.io.putOutput(chunk);
        }
        final int first = (terminal.lastRowToDisplayMax - terminal.height) * terminal.width;
        assertEquals(0xE9, terminal.buffer[first]);
        assertEquals(0x4E2D, terminal.buffer[first + 1]);
        assertEquals(0x1F600, terminal.buffer[first + 2]);
        assertEquals(3, terminal.x);
    }

    private static String decode(final byte[] bytes) {
        final Utf8Decoder decoder = new Utf8Decoder();
        final StringBuilder text = new StringBuilder();
        for (final byte value : bytes) {
            if (decoder.process(value)) {
                text.appendCodePoint(decoder.getCodepoint());
                decoder.sequenceProcessed();
            }
        }
        return text.toString();
    }
}
