package li.cil.oc2.common.blockentity.misc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.util.sound.AudioPlayback;
import li.cil.oc2.common.util.sound.DfpwmDecoder;
import li.cil.oc2.common.util.sound.SoundClientMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public final class SpeakerBlockEntity extends ModBlockEntity implements NamedDevice {
    private static final int MIN_FREQUENCY = 20;
    private static final int MAX_FREQUENCY = 20000;
    private static final int MIN_DURATION_MS = 20;
    private static final int MAX_DURATION_MS = 5000;
    private static final int PCM_CHUNK_SIZE = 4096;

    private final AudioPlayback playback = new AudioPlayback();

    public SpeakerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.SPEAKER.get(), pos, state);
    }

    @Callback
    public void beep(
            @Parameter("frequency") final float frequency,
            @Parameter("duration") final int durationMs) {
        validateTone(frequency, durationMs);
        final Level lvl = level;
        if (lvl != null && !lvl.isClientSide()) {
            SoundClientMessages.sendBeep(lvl, getBlockPos(), frequency, durationMs);
        }
    }

    @Callback
    public void playTone(
            @Parameter("frequency") final float frequency,
            @Parameter("duration") final int durationMs) {
        beep(frequency, durationMs);
    }

    /**
     * Plays raw PCM: 44.1 kHz, signed 16-bit little endian, mono.
     *
     * @return {@code true} if the data was queued, {@code false} if the speaker already holds about two
     *         seconds of audio; wait (see {@code getBufferedMs}) and send the same data again
     */
    @Callback
    public boolean write(@Parameter("data") final byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException();
        final Level lvl = level;
        if (lvl == null || lvl.isClientSide()) {
            return false;
        }
        if (!playback.acceptPcm(data.length)) {
            return false;
        }
        sendPcm(lvl, data);
        return true;
    }

    /**
     * Plays DFPWM audio recorded at 48 kHz, the rate most DFPWM files use.
     *
     * @return {@code true} if the data was queued, {@code false} if the speaker is full
     */
    @Callback
    public boolean writeDfpwm(@Parameter("data") final byte[] data) {
        return writeDfpwm(data, 48000);
    }

    /**
     * Plays DFPWM audio (one bit per sample, least significant bit first) recorded at the given rate.
     *
     * @param sampleRate the rate of the recording, from 8000 to 48000 Hz
     * @return {@code true} if the data was queued, {@code false} if the speaker is full
     */
    @Callback
    public boolean writeDfpwm(
            @Parameter("data") final byte[] data,
            @Parameter("sampleRate") final int sampleRate) {
        if (sampleRate < DfpwmDecoder.MIN_SOURCE_RATE || sampleRate > DfpwmDecoder.MAX_SOURCE_RATE) {
            throw new IllegalArgumentException("sampleRate must be between >= 8000 and <= 48000");
        }
        final Level lvl = level;
        if (lvl == null || lvl.isClientSide()) {
            return false;
        }
        final byte[] pcm = playback.decodeDfpwm(data, sampleRate);
        if (pcm.length == 0) {
            return false;
        }
        sendPcm(lvl, pcm);
        return true;
    }

    /**
     * Milliseconds of audio queued ahead of the playback position. A chunk is accepted while this plus its
     * length stays under 2000.
     */
    @Callback
    public int getBufferedMs() {
        return playback.bufferedMs();
    }

    private void sendPcm(final Level lvl, final byte[] pcm) {
        final BlockPos pos = getBlockPos();
        for (int offset = 0; offset < pcm.length; offset += PCM_CHUNK_SIZE) {
            final int length = Math.min(PCM_CHUNK_SIZE, pcm.length - offset);
            SoundClientMessages.sendPcm(
                    lvl, pos, Arrays.copyOfRange(pcm, offset, offset + length));
        }
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singletonList("speaker");
    }

    private static void validateTone(final float frequency, final int durationMs) {
        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY)
            throw new IllegalArgumentException("frequency must be between >= 20 and <= 20000");
        if (durationMs < MIN_DURATION_MS || durationMs > MAX_DURATION_MS)
            throw new IllegalArgumentException("duration must be between >= 20 and <= 5000");
    }
}