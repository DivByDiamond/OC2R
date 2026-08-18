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
    private long gameTimeCooldownExpiresAt;

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
                BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(name));
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