package li.cil.oc2.client.audio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class SoundClientManager {
    private static final Map<BlockPos, StreamingPcmSoundInstance> PCM_STREAMS = new ConcurrentHashMap<>();

    private SoundClientManager() {
    }

    public static void playTone(
            final BlockPos pos, final float frequency, final int durationMs) {
        final Vec3 center = Vec3.atCenterOf(pos);
        final ToneSoundInstance instance =
                new ToneSoundInstance(center.x, center.y, center.z, frequency, durationMs);
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    public static void streamPcm(final BlockPos pos, final byte[] pcm) {
        try {
            StreamingPcmSoundInstance instance = PCM_STREAMS.get(pos);
            if (instance == null || !instance.isDrivenByEngine(System.currentTimeMillis())) {
                stopStream(pos);
                final PcmSoundBuffer buffer = new PcmSoundBuffer();
                // The audio executor may prime the stream as soon as play() is called.
                buffer.write(pcm);
                final Vec3 center = Vec3.atCenterOf(pos);
                instance = new StreamingPcmSoundInstance(center.x, center.y, center.z, buffer);
                PCM_STREAMS.put(pos, instance);
                Minecraft.getInstance().getSoundManager().play(instance);
            } else {
                instance.getBuffer().write(pcm);
            }
        } catch (final RuntimeException ignored) {
            stopStream(pos);
        }
    }

    public static void stopStream(final BlockPos pos) {
        final StreamingPcmSoundInstance instance = PCM_STREAMS.remove(pos);
        if (instance != null) {
            instance.stopStreaming();
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }

    /** Drops every cached stream, used when the client leaves a world so no stale instance survives. */
    public static void stopAll() {
        for (final BlockPos pos : PCM_STREAMS.keySet().toArray(new BlockPos[0])) {
            stopStream(pos);
        }
    }
}
