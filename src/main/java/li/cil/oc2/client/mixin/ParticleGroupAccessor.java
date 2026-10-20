package li.cil.oc2.client.mixin;

//? if >=26.2 {
/*import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Exposes the live particles of a group (26.2 no longer has ParticleGroup.getAll) so the
// projector depth pass can let them cast shadows.
@Mixin(ParticleGroup.class)
public interface ParticleGroupAccessor {
    @Accessor(value = "particles", remap = false)
    Queue<? extends Particle> oc2r$getParticles(); // NOPMD - Namespaced Mixin accessor prevents collisions with other mods.
}
*///?} else {
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;

// Only 26.2 needs this accessor; the mixin is registered on every version, so it stays an empty mixin elsewhere.
@Mixin(ParticleEngine.class)
public interface ParticleGroupAccessor {}
//?}
