package li.cil.oc2.client.audio;

import java.util.concurrent.CompletableFuture;
import li.cil.oc2.common.util.sound.SoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

public final class StreamingPcmSoundInstance extends AbstractTickableSoundInstance
        implements CustomAudioStreamSoundInstance {
    private static final long STALE_TIMEOUT_MS = 2000;
    private static final int ATTENUATION_DISTANCE = 16;

    /** Ticks arrive every client tick while the engine owns the instance; a long gap means it was dropped. */
    static final long UNTICKED_TIMEOUT_MS = 1500;

    private final PcmSoundBuffer buffer;
    private volatile long lastTickMs = System.currentTimeMillis();

    public StreamingPcmSoundInstance(
            final double x, final double y, final double z, final PcmSoundBuffer buffer) {
        this(x, y, z, buffer, SoundEvents.SOUND_CARD_BEEP.get());
    }

    StreamingPcmSoundInstance(
            final double x, final double y, final double z, final PcmSoundBuffer buffer,
            final SoundEvent soundEvent) {
        super(soundEvent, SoundSource.BLOCKS, RandomSource.create());
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.attenuation = Attenuation.LINEAR;
        looping = true;
        // A streamed source can underrun during a slow client frame. A positive delay asks
        // SoundEngine to reopen it through its manual-loop path, retaining this PCM buffer.
        delay = 1;
        this.buffer = buffer;
    }

    @Override
    public Sound getSound() {
        return new Sound(
                //? if >=26.1 {
                /*getIdentifier(),
                *///?} else {
                getLocation(),
                //?}
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
        lastTickMs = System.currentTimeMillis();
        if (buffer.isStale(STALE_TIMEOUT_MS)) {
            stop();
        }
    }

    @Override
    public boolean canPlaySound() {
        return !isStopped();
    }

    /**
     * Whether the sound engine still drives this instance. A sound reload or a world change
     * silently drops playing instances without notifying them, so they stop ticking.
     */
    boolean isDrivenByEngine(final long nowMs) {
        return !isStopped() && nowMs - lastTickMs <= UNTICKED_TIMEOUT_MS;
    }

    void stopStreaming() {
        stop();
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    public PcmSoundBuffer getBuffer() {
        return buffer;
    }
}