package li.cil.oc2.common.blockentity.misc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
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

    @Callback
    public void write(@Parameter("data") final byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException();
        final Level lvl = level;
        if (lvl == null || lvl.isClientSide()) {
            return;
        }
        final BlockPos pos = getBlockPos();
        for (int offset = 0; offset < data.length; offset += PCM_CHUNK_SIZE) {
            final int length = Math.min(PCM_CHUNK_SIZE, data.length - offset);
            SoundClientMessages.sendPcm(
                    lvl, pos, Arrays.copyOfRange(data, offset, offset + length));
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