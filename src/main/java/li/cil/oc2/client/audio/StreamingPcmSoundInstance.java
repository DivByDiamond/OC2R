package li.cil.oc2.client.audio;

import java.util.concurrent.CompletableFuture;
import li.cil.oc2.common.util.sound.SoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

public final class StreamingPcmSoundInstance extends AbstractTickableSoundInstance {
    private static final long STALE_TIMEOUT_MS = 2000;
    private static final int ATTENUATION_DISTANCE = 16;

    private final PcmSoundBuffer buffer;

    public StreamingPcmSoundInstance(
            final double x, final double y, final double z, final PcmSoundBuffer buffer) {
        super(SoundEvents.SOUND_CARD_BEEP.get(), SoundSource.BLOCKS, RandomSource.create());
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.attenuation = Attenuation.LINEAR;
        looping = true;
        this.buffer = buffer;
    }

    @Override
    public Sound getSound() {
        return new Sound(
                getLocation(),
                ConstantFloat.of(1.0f),
                ConstantFloat.of(1.0f),
                1,
                Sound.Type.FILE,
                true,
                false,
                ATTENUATION_DISTANCE);
    }

    @Override
    public CompletableFuture<AudioStream> getStream(
            final SoundBufferLibrary soundBuffers, final Sound sound, final boolean looping) {
        return CompletableFuture.completedFuture(new PcmAudioStream(buffer));
    }

    @Override
    public void tick() {
        if (buffer.isStale(STALE_TIMEOUT_MS)) {
            stop();
        }
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    public PcmSoundBuffer getBuffer() {
        return buffer;
    }
}