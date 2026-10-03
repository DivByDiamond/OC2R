package li.cil.oc2.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.concurrent.CompletableFuture;
import li.cil.oc2.client.audio.CustomAudioStreamSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge lets a sound instance supply its own audio stream; vanilla always loads the stream from the
 * sound file. Sound instances implementing {@link CustomAudioStreamSoundInstance} (the sound card
 * tones and PCM streams) get the same behaviour here.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @WrapOperation(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream("
                            + "Lnet/minecraft/resources/ResourceLocation;Z)"
                            + "Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<AudioStream> oc2r$customStream(
            final SoundBufferLibrary library,
            final ResourceLocation location,
            final boolean looping,
            final Operation<CompletableFuture<AudioStream>> original,
            @Local(argsOnly = true) final SoundInstance instance) {
        if (instance instanceof final CustomAudioStreamSoundInstance custom) {
            return custom.getStream(library, instance.getSound(), looping);
        }
        return original.call(library, location, looping);
    }
}
