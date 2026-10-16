package li.cil.oc2.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

public final class PcmAudioStream implements AudioStream {
    // OpenAL primes four buffers and asks for a second each. Keep both PCM and silence
    // read-ahead below the instance's two-second idle timeout, without a multi-second backlog.
    private static final int MAX_BUFFER_BYTES = ToneAudioStream.SAMPLE_RATE * 2 * 100 / 1000;

    private final PcmSoundBuffer buffer;

    public PcmAudioStream(final PcmSoundBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public AudioFormat getFormat() {
        return new AudioFormat(ToneAudioStream.SAMPLE_RATE, 16, 1, true, false);
    }

    @Override
    public ByteBuffer read(final int size) throws IOException {
        if (size <= 0) {
            return ByteBuffer.allocateDirect(0);
        }
        final int boundedSize = Math.min(size, MAX_BUFFER_BYTES);
        final byte[] out = new byte[boundedSize];
        final int read = buffer.read(out);
        // OpenAL reads the samples through their native address, so the buffer must be direct (a heap
        // buffer crashes the JVM in alBufferData); without data a block of silence keeps the stream alive.
        final ByteBuffer direct = ByteBuffer.allocateDirect(read > 0 ? read : boundedSize).order(ByteOrder.LITTLE_ENDIAN);
        direct.put(out, 0, direct.capacity());
        direct.flip();
        return direct;
    }

    @Override
    public void close() {
    }
}