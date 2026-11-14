package li.cil.oc2.common.util.sound;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DfpwmDecoderTest {
    private static final int PRECISION = 10;

    /** Reference encoder for DFPWM1a, used to produce test input. */
    private static byte[] encode(final int... levels) {
        final byte[] out = new byte[levels.length / 8];
        int charge = 0;
        int strength = 0;
        boolean previous = false;
        for (int i = 0; i < out.length * 8; i++) {
            final int level = levels[i];
            final boolean bit = level > charge || (level == charge && charge == 127);
            final int target = bit ? 127 : -128;
            int next = charge + ((strength * (target - charge) + (1 << (PRECISION - 1))) >> PRECISION);
            if (next == charge && next != target) {
                next += bit ? 1 : -1;
            }
            final int limit = bit == previous ? (1 << PRECISION) - 1 : 0;
            int nextStrength = strength;
            if (strength != limit) {
                nextStrength += bit == previous ? 1 : -1;
            }
            nextStrength = Math.max(nextStrength, 2 << (PRECISION - 8));
            charge = next;
            strength = nextStrength;
            previous = bit;
            if (bit) {
                out[i / 8] |= (byte) (1 << (i % 8));
            }
        }
        return out;
    }

    private static int[] sine(final double frequency, final int rate, final int samples, final int amplitude) {
        final int[] levels = new int[samples];
        for (int i = 0; i < samples; i++) {
            levels[i] = (int) Math.round(amplitude * Math.sin(2 * Math.PI * frequency * i / rate));
        }
        return levels;
    }

    private static short[] samples(final byte[] pcm) {
        final short[] out = new short[pcm.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (short) ((pcm[2 * i] & 0xFF) | (pcm[2 * i + 1] << 8));
        }
        return out;
    }

    private static double correlation(final short[] decoded, final int rate, final double frequency, final int skip) {
        double sumXY = 0;
        double sumXX = 0;
        double sumYY = 0;
        for (int i = skip; i < decoded.length; i++) {
            final double reference = Math.sin(2 * Math.PI * frequency * i / rate);
            sumXY += decoded[i] * reference;
            sumXX += decoded[i] * (double) decoded[i];
            sumYY += reference * reference;
        }
        return sumXY / Math.sqrt(sumXX * sumYY);
    }

    @Test
    void decodedLengthMatchesTheAnnouncedLength() {
        final byte[] data = new byte[1000];
        for (final int rate : new int[] {8000, 22050, 44100, 48000}) {
            assertLengthMatches(data, rate);
        }
    }

    private static void assertLengthMatches(final byte[] data, final int rate) {
        final byte[] pcm = new DfpwmDecoder().decode(data, rate);
        final int expected = DfpwmDecoder.pcmLength(data.length, rate);
        assertTrue(Math.abs(pcm.length - expected) <= 4, rate + " Hz: " + pcm.length + " vs " + expected);
        assertEquals(0, pcm.length % 2);
    }

    @Test
    void aToneSurvivesAtTheNativeRate() {
        final byte[] encoded = encode(sine(300, 44100, 44100, 100));
        final short[] decoded = samples(new DfpwmDecoder().decode(encoded, 44100));
        assertTrue(correlation(decoded, 44100, 300, 2000) > 0.95);
        int peak = 0;
        for (final short s : decoded) {
            peak = Math.max(peak, Math.abs(s));
        }
        assertTrue(peak > 10000 && peak < 32767, "peak " + peak);
    }

    @Test
    void aToneKeepsItsPitchWhenTheRecordingRateDiffers() {
        final byte[] encoded = encode(sine(300, 48000, 48000, 100));
        final byte[] pcm = new DfpwmDecoder().decode(encoded, 48000);
        // One second of recording plays for one second at 44.1 kHz.
        assertTrue(Math.abs(pcm.length / 2 - 44100) <= 2);
        assertTrue(correlation(samples(pcm), 44100, 300, 2000) > 0.95);
    }

    @Test
    void decodingInChunksGivesTheSameAudioAsDecodingAtOnce() {
        final byte[] encoded = encode(sine(440, 48000, 48000, 90));
        final byte[] whole = new DfpwmDecoder().decode(encoded, 48000);

        final DfpwmDecoder chunked = new DfpwmDecoder();
        final java.io.ByteArrayOutputStream joined = new java.io.ByteArrayOutputStream();
        for (int offset = 0; offset < encoded.length; offset += 777) {
            final int end = Math.min(encoded.length, offset + 777);
            joined.writeBytes(chunked.decode(java.util.Arrays.copyOfRange(encoded, offset, end), 48000));
        }
        assertArrayEquals(whole, joined.toByteArray());
    }

    @Test
    void resetStartsTheNextStreamFromSilence() {
        final DfpwmDecoder decoder = new DfpwmDecoder();
        final byte[] encoded = encode(sine(300, 44100, 4410, 100));
        final byte[] first = decoder.decode(encoded, 44100);
        decoder.reset();
        assertArrayEquals(first, decoder.decode(encoded, 44100));
    }

    @Test
    void ratesOutsideTheSupportedRangeAreRejected() {
        final DfpwmDecoder decoder = new DfpwmDecoder();
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(new byte[8], 7999));
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(new byte[8], 48001));
    }
}
