package li.cil.oc2.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

public final class ToneAudioStream implements AudioStream {
    public static final int SAMPLE_RATE = 44100;
    private static final int MIN_DURATION_MS = 20;
    private static final int MAX_DURATION_MS = 5000;
    private static final int FADE_DURATION_MS = 5;

    private final float frequency;
    private final long totalSamples;
    private final long totalBytes;
    private final int fadeSamples;

    private long position;
    private boolean closed;

    public ToneAudioStream(final float frequency, final int durationMs) {
        this.frequency = frequency;
        final int clampedDuration =
                Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, durationMs));
        totalSamples = (long) clampedDuration * SAMPLE_RATE / 1000;
        totalBytes = totalSamples * 2;
        fadeSamples = SAMPLE_RATE * FADE_DURATION_MS / 1000;
    }

    @Override
    public AudioFormat getFormat() {
        return new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    }

    @Override
    public ByteBuffer read(final int size) throws IOException {
        if (closed || position >= totalBytes) {
            return null;
        }
        if (size <= 0) {
            return ByteBuffer.allocate(0);
        }

        int bytesToWrite = (int) Math.min(size, totalBytes - position);
        bytesToWrite -= bytesToWrite & 1;

        final ByteBuffer buffer = ByteBuffer.allocate(bytesToWrite);
        final int samplesToWrite = bytesToWrite / 2;
        final long startSample = position / 2;
        for (int i = 0; i < samplesToWrite; i++) {
            final long sampleIndex = startSample + i;
            final float envelope = envelope(sampleIndex);
            final short sample =
                    (short)
                            (Math.sin(2 * Math.PI * frequency * sampleIndex / SAMPLE_RATE)
                                            * Short.MAX_VALUE
                                            * envelope);
            buffer.putShort(sample);
        }
        position += bytesToWrite;
        return buffer;
    }

    @Override
    public void close() {
        closed = true;
    }

    private float envelope(final long sampleIndex) {
        if (sampleIndex < fadeSamples) {
            return (float) sampleIndex / fadeSamples;
        }
        final long samplesFromEnd = totalSamples - sampleIndex;
        if (samplesFromEnd < fadeSamples) {
            return Math.min(1f, (float) samplesFromEnd / fadeSamples);
        }
        return 1f;
    }
}