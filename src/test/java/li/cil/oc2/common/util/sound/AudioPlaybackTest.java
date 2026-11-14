package li.cil.oc2.common.util.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AudioPlaybackTest {
    private static final long MS = 1_000_000L;

    private AtomicLong now;
    private AudioPlayback playback;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(5_000L * MS);
        playback = new AudioPlayback(now::get);
    }

    private static byte[] dfpwmOfMs(final int ms, final int rate) {
        return new byte[ms * rate / 1000 / 8];
    }

    @Test
    void anIdleSpeakerHasNothingQueued() {
        assertEquals(0, playback.bufferedMs());
    }

    @Test
    void writtenAudioCountsAsQueuedUntilItHasPlayed() {
        assertTrue(playback.decodeDfpwm(dfpwmOfMs(800, 48000), 48000).length > 0);
        assertEquals(800, playback.bufferedMs(), 5);
        now.addAndGet(300 * MS);
        assertEquals(500, playback.bufferedMs(), 5);
        now.addAndGet(600 * MS);
        assertEquals(0, playback.bufferedMs());
    }

    @Test
    void aFullQueueRefusesMoreAndConsumesNothing() {
        assertTrue(playback.decodeDfpwm(dfpwmOfMs(1000, 48000), 48000).length > 0);
        assertTrue(playback.decodeDfpwm(dfpwmOfMs(900, 48000), 48000).length > 0);
        final int before = playback.bufferedMs();
        assertEquals(0, playback.decodeDfpwm(dfpwmOfMs(500, 48000), 48000).length);
        assertEquals(before, playback.bufferedMs());

        now.addAndGet(600 * MS);
        assertTrue(playback.decodeDfpwm(dfpwmOfMs(500, 48000), 48000).length > 0);
    }

    @Test
    void pcmIsAccountedAtTheOutputRate() {
        // 44100 samples of 16 bit are one second.
        assertTrue(playback.acceptPcm(44100 * 2));
        assertEquals(1000, playback.bufferedMs(), 5);
        assertTrue(playback.acceptPcm(44100));
        assertFalse(playback.acceptPcm(44100 * 2));
    }

    @Test
    void notedPcmIsCountedWithoutBeingRefused() {
        playback.notePcm(44100 * 2 * 3);
        assertEquals(3000, playback.bufferedMs(), 5);
        assertFalse(playback.acceptPcm(2));
    }

    @Test
    void oversizedOrEmptyChunksAreErrorsNotRefusals() {
        assertThrows(IllegalArgumentException.class,
                () -> playback.decodeDfpwm(new byte[AudioPlayback.MAX_DFPWM_BYTES + 1], 48000));
        assertThrows(IllegalArgumentException.class, () -> playback.decodeDfpwm(new byte[0], 48000));
        // 8192 bytes at 8 kHz is 8.2 seconds, which can never fit the queue.
        assertThrows(IllegalArgumentException.class, () -> playback.decodeDfpwm(new byte[8192], 8000));
        assertThrows(IllegalArgumentException.class, () -> playback.acceptPcm(44100 * 2 * 3));
    }

    @Test
    void theDecoderRestartsAfterALongSilence() {
        final byte[] loud = new byte[2048];
        java.util.Arrays.fill(loud, (byte) 0xFF);
        final byte[] first = playback.decodeDfpwm(loud, 44100);
        now.addAndGet(5_000 * MS);
        assertEquals(java.util.Arrays.toString(first), java.util.Arrays.toString(playback.decodeDfpwm(loud, 44100)));
    }

    @Test
    void aShortPauseKeepsTheDecoderState() {
        final byte[] loud = new byte[2048];
        java.util.Arrays.fill(loud, (byte) 0xFF);
        final byte[] first = playback.decodeDfpwm(loud, 44100);
        now.addAndGet(500 * MS);
        final byte[] second = playback.decodeDfpwm(loud, 44100);
        assertFalse(java.util.Arrays.equals(first, second));
    }
}
