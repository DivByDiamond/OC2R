package li.cil.oc2.client.audio;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;

/**
 * A sound instance that provides its own audio stream instead of loading a file. NeoForge patches
 * the same method into vanilla's {@code SoundInstance} and calls it itself; on Fabric the sound engine
 * mixin calls it for instances implementing this interface.
 */
public interface CustomAudioStreamSoundInstance {
    CompletableFuture<AudioStream> getStream(
            SoundBufferLibrary soundBuffers, Sound sound, boolean looping);
}
