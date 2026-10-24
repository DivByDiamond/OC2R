//? if >=26.1 {
/*
package li.cil.oc2.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

// The modern projector owns a dedicated depth pass and does not recurse through vanilla rendering.
@Mixin(net.minecraft.client.renderer.fog.FogRenderer.class)
public abstract class FogRendererMixin {}
*///?} else {

package li.cil.oc2.fabric.mixin.client;

import li.cil.oc2.client.renderer.projector.ProjectorDepthRenderer;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disables fog while the projector depth pass renders the level (NeoForge: {@code RenderFog} event). */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void oc2r$noFogForProjectorDepth(final CallbackInfo ignoredCi) {
        if (ProjectorDepthRenderer.isRenderingProjectorDepth) {
            FogRenderer.setupNoFog();
        }
    }
}
//?}
