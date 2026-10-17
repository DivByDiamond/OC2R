package li.cil.oc2.client.mixin;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.1 && <26.2 {
/*import java.util.Map;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

/** Exposes live particles so the projector depth pass can let them cast shadows. */
@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    //? if >=26.1 && <26.2 {
/*    @Accessor(value = "particles", remap = false)
    Map<ParticleRenderType, ParticleGroup<?>> oc2r$getParticles();
*///?}
}
