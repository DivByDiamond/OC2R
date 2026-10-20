package li.cil.oc2.client.mixin;

//? if >=26.1 && <26.2 {
/*import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Preserves the original cutout texture when rendering block-entity projector shadows.
@Mixin(RenderType.class)
public interface ProjectorRenderTypeAccessor {
    @Accessor(value = "state", remap = false)
    RenderSetup oc2r$getSetup(); // NOPMD - Namespaced Mixin accessor prevents collisions with other mods.
}
*///?} else {
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;

// No access is needed outside 26.1; keep the shared mixin registration valid.
@Mixin(ParticleEngine.class)
public interface ProjectorRenderTypeAccessor {}
//?}
