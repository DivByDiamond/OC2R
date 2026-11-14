package li.cil.oc2.common.util.sound;

import java.io.ByteArrayOutputStream;

/**
 * Stateful decoder for DFPWM1a audio (one bit per sample, least significant bit first).
 *
 * <p>The decoded samples are turned into 16-bit little endian mono PCM at {@link #OUTPUT_RATE}
 * with linear interpolation, so a stream recorded at another rate (48 kHz is common) plays at the right pitch.
 * The decoder keeps its state between calls so that a long stream can be fed in chunks.
 */
public final class DfpwmDecoder {
    public static final int OUTPUT_RATE = 44100;
    public static final int MIN_SOURCE_RATE = 8000;
    public static final int MAX_SOURCE_RATE = 48000;

    private static final int PRECISION = 10;
    private static final int MIN_STRENGTH = 2 << (PRECISION - 8);
    private static final int LOW_PASS_STRENGTH = 140;

    private int charge;
    private int strength;
    private boolean previousBit;
    private int lowPass;

    private int previousSample;
    private int resampleAccumulator;

    /** Forgets everything learned from the previous stream. */
    public void reset() {
        charge = 0;
        strength = 0;
        previousBit = false;
        lowPass = 0;
        previousSample = 0;
        resampleAccumulator = 0;
    }

    /** Number of PCM bytes that decoding {@code dfpwmBytes} at {@code sourceRate} produces, rounded up. */
    public static int pcmLength(final int dfpwmBytes, final int sourceRate) {
        final long samples = (long) dfpwmBytes * 8;
        return (int) ((samples * OUTPUT_RATE + sourceRate - 1) / sourceRate) * 2;
    }

    public byte[] decode(final byte[] dfpwm, final int sourceRate) {
        if (sourceRate < MIN_SOURCE_RATE || sourceRate > MAX_SOURCE_RATE) {
            throw new IllegalArgumentException(
                    "sample rate must be between " + MIN_SOURCE_RATE + " and " + MAX_SOURCE_RATE);
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream(pcmLength(dfpwm.length, sourceRate));
        for (final byte value : dfpwm) {
            int bits = value;
            for (int i = 0; i < 8; i++, bits >>= 1) {
                resample(nextSample((bits & 1) != 0), sourceRate, out);
            }
        }
        return out.toByteArray();
    }

    private int nextSample(final boolean bit) {
        final int target = bit ? 127 : -128;
        int nextCharge = charge + ((strength * (target - charge) + (1 << (PRECISION - 1))) >> PRECISION);
        if (nextCharge == charge && nextCharge != target) {
            nextCharge += bit ? 1 : -1;
        }

        final boolean repeated = bit == previousBit;
        final int limit = repeated ? (1 << PRECISION) - 1 : 0;
        int nextStrength = strength;
        if (strength != limit) {
            nextStrength += repeated ? 1 : -1;
        }
        nextStrength = Math.max(nextStrength, MIN_STRENGTH);

        // Averaging with the previous level when the bit flips removes the click of a sudden step.
        final int smoothed = repeated ? nextCharge : (nextCharge + charge + 1) >> 1;
        lowPass += ((smoothed - lowPass) * LOW_PASS_STRENGTH + 0x80) >> 8;

        charge = nextCharge;
        strength = nextStrength;
        previousBit = bit;
        return lowPass << 8;
    }

    private void resample(final int sample, final int sourceRate, final ByteArrayOutputStream out) {
        // One input interval is OUTPUT_RATE units long and one output interval is sourceRate units.
        while (resampleAccumulator < OUTPUT_RATE) {
            final long interpolated = previousSample
                    + (long) (sample - previousSample) * resampleAccumulator / OUTPUT_RATE;
            final int value = (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, interpolated));
            out.write(value & 0xFF);
            out.write((value >> 8) & 0xFF);
            resampleAccumulator += sourceRate;
        }
        resampleAccumulator -= OUTPUT_RATE;
        previousSample = sample;
    }
}
