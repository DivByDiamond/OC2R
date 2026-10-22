package li.cil.oc2.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.ByteBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.junit.jupiter.api.Test;

class StreamingPcmSoundInstanceTest {
    @Test
    @SuppressFBWarnings(
            value = "NP_NONNULL_PARAM_VIOLATION",
            justification =
                    "the buffer library and the sound are not used by the streaming instance, so"
                        + " the test passes null for them on purpose")
    void manualRestartPreservesPcmQueuedAfterAnUnderrun() throws Exception {
        final PcmSoundBuffer buffer = new PcmSoundBuffer();
        final StreamingPcmSoundInstance instance = instance(buffer);
        final PcmAudioStream first = (PcmAudioStream) instance.getStream(null, null, false).join();
        first.read(ToneAudioStream.SAMPLE_RATE * 2);
        first.close(); // Channel.destroy closes the old stream after an underrun.
        buffer.write(new byte[]{1, 2, 3, 4});

        assertTrue(instance.isLooping());
        assertTrue(instance.getDelay() > 0, "SoundEngine must retry a stopped channel via manual looping");
        assertTrue(instance.canPlaySound());
        final PcmAudioStream restarted = (PcmAudioStream) instance.getStream(null, null, false).join();
        final ByteBuffer pcm = restarted.read(ToneAudioStream.SAMPLE_RATE * 2);
        assertEquals(4, pcm.remaining());
        assertEquals(1, pcm.get());
        assertEquals(2, pcm.get());
        assertEquals(3, pcm.get());
        assertEquals(4, pcm.get());
    }

    @Test
    void explicitlyStoppedStreamCannotBeRestartedByTheSoundEngine() {
        final StreamingPcmSoundInstance instance = instance(new PcmSoundBuffer());

        instance.stopStreaming();

        assertTrue(instance.isStopped());
        assertFalse(instance.canPlaySound());
    }

    @Test
    void instanceDroppedByTheEngineIsNotReused() {
        final PcmSoundBuffer buffer = new PcmSoundBuffer();
        buffer.write(new byte[]{1, 2});
        final StreamingPcmSoundInstance instance = instance(buffer);
        final long now = System.currentTimeMillis();

        assertTrue(instance.isDrivenByEngine(now));
        assertFalse(instance.isDrivenByEngine(now + StreamingPcmSoundInstance.UNTICKED_TIMEOUT_MS + 1000));
        instance.tick();
        assertTrue(instance.isDrivenByEngine(System.currentTimeMillis()));
    }

    private static StreamingPcmSoundInstance instance(final PcmSoundBuffer buffer) {
        final SoundEvent sound = SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("oc2r", "pcm_test"));
        return new StreamingPcmSoundInstance(0, 0, 0, buffer, sound);
    }
}
