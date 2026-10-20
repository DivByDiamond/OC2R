package li.cil.oc2.client.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PcmAudioStreamLatencyTest {
    @Test
    void longBurstRemainsBufferedUntilPlaybackRequestsIt() throws IOException {
        final int bytesPerSecond = ToneAudioStream.SAMPLE_RATE * 2;
        final byte[] data = new byte[bytesPerSecond * 3];
        for (int index = 0; index < data.length; index++) {
            data[index] = (byte) index;
        }
        final PcmSoundBuffer source = new PcmSoundBuffer();
        source.write(data);
        final PcmAudioStream stream = new PcmAudioStream(source);
        int consumed = 0;
        for (int index = 0; index < 4; index++) {
            final ByteBuffer pcm = stream.read(bytesPerSecond);
            final byte[] actual = new byte[pcm.remaining()]; // NOPMD - Each test case needs independent fixture inputs.
            pcm.get(actual);
            assertArrayEquals(Arrays.copyOfRange(data, consumed, consumed + actual.length), actual);
            consumed += actual.length;
        }
        assertTrue(consumed <= bytesPerSecond / 2,
                "OpenAL must not prefetch seconds of PCM and let the source time out early");
        final byte[] remaining = new byte[data.length - consumed];
        assertEquals(remaining.length, source.read(remaining));
        assertArrayEquals(Arrays.copyOfRange(data, consumed, data.length), remaining);
    }

    @Test
    void initialOpenAlPrefillCannotQueueSecondsOfSilenceAheadOfIncomingPcm() throws IOException {
        final PcmSoundBuffer source = new PcmSoundBuffer();
        final PcmAudioStream stream = new PcmAudioStream(source);
        // Channel.attachBufferStream primes four buffers, requesting one second per buffer.
        // Network packets can arrive after this prefill. A multi-second silence backlog delays
        // their playback past StreamingPcmSoundInstance's two-second idle timeout.
        final int bytesPerSecond = ToneAudioStream.SAMPLE_RATE * 2;
        int queuedSilence = 0;
        for (int index = 0; index < 4; index++) {
            final ByteBuffer silence = stream.read(bytesPerSecond);
            queuedSilence += silence.remaining();
            while (silence.hasRemaining()) {
                assertEquals(0, silence.get());
            }
        }
        assertTrue(queuedSilence <= bytesPerSecond / 2,
                "OpenAL prefill must leave at most 500 ms ahead of incoming PCM, got "
                        + queuedSilence + " bytes");

        source.write(new byte[]{1, 2, 3, 4});
        final ByteBuffer incoming = stream.read(bytesPerSecond);
        assertEquals(4, incoming.remaining());
        assertEquals(1, incoming.get());
        assertEquals(2, incoming.get());
        assertEquals(3, incoming.get());
        assertEquals(4, incoming.get());
    }
}
