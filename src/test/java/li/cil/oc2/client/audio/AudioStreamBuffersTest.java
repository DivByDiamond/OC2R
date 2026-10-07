package li.cil.oc2.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

/**
 * OpenAL reads the samples handed to {@code Channel.attachBufferStream} through their native address,
 * so a heap buffer crashes the JVM in {@code alBufferData}, and an unflipped buffer carries no data.
 * Found by playing a sound card tone in a real client.
 */
class AudioStreamBuffersTest {
    @Test
    void toneBuffersAreDirectFlippedAndLittleEndian() throws IOException {
        final ToneAudioStream stream = new ToneAudioStream(440f, 100);
        final ByteBuffer buffer = stream.read(4096);

        assertNotNull(buffer);
        assertTrue(buffer.isDirect(), "heap buffers crash OpenAL");
        assertEquals(4096, buffer.remaining());
        assertEquals(ByteOrder.LITTLE_ENDIAN, buffer.order());

        // 440 Hz at 44.1 kHz peaks at sample 226 (two and a quarter periods in), past the fade in,
        // as a positive little endian sample.
        final short peak = buffer.getShort(2 * 226);
        assertTrue(peak > 30000, "unexpected sample " + peak);
    }

    @Test
    void toneEndsAfterItsDuration() throws IOException {
        final ToneAudioStream stream = new ToneAudioStream(440f, 20);
        int total = 0;
        for (ByteBuffer buffer = stream.read(1024); buffer != null; buffer = stream.read(1024)) {
            total += buffer.remaining();
        }
        assertEquals(44100 * 20 / 1000 * 2, total);
    }

    @Test
    void pcmBuffersAreDirectAndFlipped() throws IOException {
        final PcmSoundBuffer source = new PcmSoundBuffer();
        source.write(new byte[]{1, 2, 3, 4});
        final PcmAudioStream stream = new PcmAudioStream(source);

        final ByteBuffer data = stream.read(1024);
        assertTrue(data.isDirect(), "heap buffers crash OpenAL");
        assertEquals(4, data.remaining());
        assertEquals(1, data.get(0));
        assertEquals(4, data.get(3));

        // Without data the stream plays silence so that it stays alive.
        final ByteBuffer silence = stream.read(1024);
        assertTrue(silence.isDirect());
        assertEquals(1024, silence.remaining());
    }
}
