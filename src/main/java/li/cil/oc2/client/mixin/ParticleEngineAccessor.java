package li.cil.oc2.client.mixin;

//? if >=26.1 {
/*import java.util.Map;
*///?}
import net.minecraft.client.particle.ParticleEngine;
//? if >=26.1 {
/*import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
*///?}
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.1 {
/*import org.spongepowered.asm.mixin.gen.Accessor;
*///?}

/** Exposes live particles so the projector depth pass can let them cast shadows. */
@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    //? if >=26.1 {
/*    @Accessor(value = "particles", remap = false)
    Map<ParticleRenderType, ParticleGroup<?>> oc2r$getParticles(); // NOPMD - Namespaced Mixin accessor prevents collisions with other mods.
*///?}
}
