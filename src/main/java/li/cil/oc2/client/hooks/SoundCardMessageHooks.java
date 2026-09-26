package li.cil.oc2.client.hooks;

import li.cil.oc2.client.audio.SoundClientManager;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Client-side entry points for sound card network messages, kept out of common message classes. */
@OnlyIn(Dist.CLIENT)
public final class SoundCardMessageHooks {
    private SoundCardMessageHooks() {}

    public static void playTone(final BlockPos pos, final float frequency, final int durationMs) {
        SoundClientManager.playTone(pos, frequency, durationMs);
    }

    public static void streamPcm(final BlockPos pos, final byte[] pcm) {
        SoundClientManager.streamPcm(pos, pcm);
    }
}
