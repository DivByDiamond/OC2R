package li.cil.oc2.common.bus.device.rpc.item.card;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.bus.device.rpc.item.AbstractItemRPCDevice;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.util.sound.AudioPlayback;
import li.cil.oc2.common.util.sound.DfpwmDecoder;
import li.cil.oc2.common.util.sound.SoundClientMessages;
import li.cil.oc2.common.util.tick.TickUtils;
import li.cil.oc2.common.util.world.BlockLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

@SuppressWarnings("unused")
public final class SoundCardItemDevice extends AbstractItemRPCDevice {
    private final int COOLDOWN_IN_TICKS =
            TickUtils.toTicks(Duration.ofSeconds(Config.soundCardCoolDownSeconds));
    private static final int MAX_FIND_RESULTS = 25;
    private static final int MIN_FREQUENCY = 20;
    private static final int MAX_FREQUENCY = 20000;
    private static final int MIN_DURATION_MS = 20;
    private static final int MAX_DURATION_MS = 5000;
    private static final int PCM_CHUNK_SIZE = 4096;

    private final Supplier<Optional<BlockLocation>> location;
    private final AudioPlayback playback = new AudioPlayback();
    private long gameTimeCooldownExpiresAt;
    // PCM token bucket; starts full so short bursts right after mounting work.
    private long pcmBudgetBytes = Config.soundCardPcmBytesPerSecond;
    private long pcmBudgetRefillAtNanos = System.nanoTime();

    public SoundCardItemDevice(
            final ItemStack identity, final Supplier<Optional<BlockLocation>> location) {
        super(identity, "sound");
        this.location = location;
    }

    @Callback
    public void playSound(@Nullable @Parameter("name") final String name) {
        playSound(name, 1, 1);
    }

    @Callback
    public void playSound(
            @Nullable @Parameter("name") final String name,
            @Parameter("volume") final float volume) {
        playSound(name, volume, 1);
    }

    @Callback
    public void playSound(
            @Nullable @Parameter("name") final String name,
            @Parameter("volume") final float volume,
            @Parameter("pitch") final float pitch) {
        validateParameters(name, volume, pitch);
        if (volume == 0) return;

        location.get()
                .ifPresent(
                        loc ->
                                loc.tryGetLevel()
                                        .ifPresent(
                                                level ->
                                                        playSoundAt(
                                                                loc, level, name, volume, pitch)));
    }

    private static void validateParameters(
            @Nullable final String name, final float volume, final float pitch) {
        if (name == null) throw new IllegalArgumentException();
        if (volume < 0 || volume > 1.0f)
            throw new IllegalArgumentException("volume must be between >= 0 and <= 1");
        if (pitch < 0.5f || pitch > 2.0f)
            throw new IllegalArgumentException("pitch must be between >= 0.5 and <= 2");
    }

    private void playSoundAt(
            final BlockLocation location,
            final LevelAccessor level,
            final String name,
            final float volume,
            final float pitch) {
        if (!(level instanceof final ServerLevel serverLevel)) {
            return;
        }

        final long gameTime = serverLevel.getGameTime();
        if (gameTime < gameTimeCooldownExpiresAt) {
            return;
        }

        gameTimeCooldownExpiresAt = gameTime + COOLDOWN_IN_TICKS;

        final SoundEvent soundEvent =
                //? if >=26.1 {
                /*BuiltInRegistries.SOUND_EVENT.getValue(ResourceLocation.parse(name));
                *///?} else {
                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(name));
                //?}
        if (soundEvent == null) throw new IllegalArgumentException("Sound not found.");
        serverLevel.playSound(
                null,
                location.blockPos(),
                soundEvent,
                SoundSource.BLOCKS,
                volume,
                pitch);
    }

    @Callback
    public List<String> findSound(@Nullable @Parameter("name") final String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException();

        final List<String> matches = new ArrayList<>();

        for (final ResourceLocation key : BuiltInRegistries.SOUND_EVENT.keySet()) {
            final String keyName = key.toString();
            if (keyName.contains(name)) {
                matches.add(keyName);
                if (matches.size() >= MAX_FIND_RESULTS) {
                    break;
                }
            }
        }

        return matches;
    }

    @Callback
    public void beep(
            @Parameter("frequency") final float frequency,
            @Parameter("duration") final int durationMs) {
        validateTone(frequency, durationMs);
        sendTone(frequency, durationMs);
    }

    @Callback
    public void playTone(
            @Parameter("frequency") final float frequency,
            @Parameter("duration") final int durationMs) {
        beep(frequency, durationMs);
    }

    @Callback
    public void write(@Parameter("data") final byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException();
        refillPcmBudget();
        if (data.length > pcmBudgetBytes) {
            throw new IllegalArgumentException(
                    "PCM byte rate limit exceeded, slow down or write less per call");
        }
        pcmBudgetBytes -= data.length;
        playback.notePcm(data.length);
        location.get()
                .ifPresent(
                        loc -> {
                            final BlockPos pos = loc.blockPos();
                            loc.tryGetLevel()
                                    .ifPresent(
                                            level -> {
                                                if (!(level instanceof final Level lvl)) {
                                                    return;
                                                }
                                                for (int offset = 0;
                                                        offset < data.length;
                                                        offset += PCM_CHUNK_SIZE) {
                                                    final int length =
                                                            Math.min(
                                                                    PCM_CHUNK_SIZE,
                                                                    data.length - offset);
                                                    SoundClientMessages.sendPcm(
                                                            lvl,
                                                            pos,
                                                            Arrays.copyOfRange(
                                                                    data, offset, offset + length));
                                                }
                                            });
                        });
    }

    /**
     * Plays DFPWM audio recorded at 48 kHz.
     *
     * @return {@code true} if the data was queued, {@code false} if about two seconds are already queued
     */
    @Callback
    public boolean writeDfpwm(@Parameter("data") final byte[] data) {
        return writeDfpwm(data, 48000);
    }

    /**
     * Plays DFPWM audio (one bit per sample, least significant bit first) recorded at the given rate.
     *
     * @param sampleRate the rate of the recording, from 8000 to 48000 Hz
     * @return {@code true} if the data was queued, {@code false} if the queue is full
     */
    @Callback
    public boolean writeDfpwm(
            @Parameter("data") final byte[] data,
            @Parameter("sampleRate") final int sampleRate) {
        if (sampleRate < DfpwmDecoder.MIN_SOURCE_RATE || sampleRate > DfpwmDecoder.MAX_SOURCE_RATE) {
            throw new IllegalArgumentException("sampleRate must be between >= 8000 and <= 48000");
        }
        final byte[] pcm = playback.decodeDfpwm(data, sampleRate);
        if (pcm.length == 0) {
            return false;
        }
        location.get()
                .ifPresent(
                        loc ->
                                loc.tryGetLevel()
                                        .ifPresent(
                                                level -> {
                                                    if (!(level instanceof final Level lvl)) {
                                                        return;
                                                    }
                                                    for (int offset = 0;
                                                            offset < pcm.length;
                                                            offset += PCM_CHUNK_SIZE) {
                                                        final int length =
                                                                Math.min(
                                                                        PCM_CHUNK_SIZE,
                                                                        pcm.length - offset);
                                                        SoundClientMessages.sendPcm(
                                                                lvl,
                                                                loc.blockPos(),
                                                                Arrays.copyOfRange(
                                                                        pcm, offset, offset + length));
                                                    }
                                                }));
        return true;
    }

    /** Milliseconds of audio queued ahead of the playback position. */
    @Callback
    public int getBufferedMs() {
        return playback.bufferedMs();
    }

    /**
     * Token bucket limiting PCM streaming to {@code Config.soundCardPcmBytesPerSecond}
     * bytes per second per device, refilled continuously. Prevents a guest from
     * flooding all clients tracking the chunk with network messages.
     */
    private void refillPcmBudget() {
        final long now = System.nanoTime();
        final long elapsed = now - pcmBudgetRefillAtNanos;
        if (elapsed > 0) {
            pcmBudgetBytes =
                    Math.min(
                            Config.soundCardPcmBytesPerSecond,
                            pcmBudgetBytes + elapsed / 1_000_000L * Config.soundCardPcmBytesPerSecond / 1000);
            pcmBudgetRefillAtNanos = now;
        }
    }

    private void sendTone(final float frequency, final int durationMs) {
        location.get()
                .ifPresent(
                        loc -> {
                            final BlockPos pos = loc.blockPos();
                            loc.tryGetLevel()
                                    .ifPresent(
                                            level -> {
                                                if (!(level instanceof final Level lvl)) {
                                                    return;
                                                }
                                                SoundClientMessages.sendBeep(
                                                        lvl, pos, frequency, durationMs);
                                            });
                        });
    }

    private static void validateTone(final float frequency, final int durationMs) {
        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY)
            throw new IllegalArgumentException("frequency must be between >= 20 and <= 20000");
        if (durationMs < MIN_DURATION_MS || durationMs > MAX_DURATION_MS)
            throw new IllegalArgumentException("duration must be between >= 20 and <= 5000");
    }
}