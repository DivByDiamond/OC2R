package li.cil.oc2.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

public final class PcmAudioStream implements AudioStream {
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
            return ByteBuffer.allocate(0);
        }
        final byte[] out = new byte[size];
        final int read = buffer.read(out);
        if (read > 0) {
            return ByteBuffer.wrap(out, 0, read);
        }
        return ByteBuffer.wrap(new byte[size]);
    }

    @Override
    public void close() {
    }
}