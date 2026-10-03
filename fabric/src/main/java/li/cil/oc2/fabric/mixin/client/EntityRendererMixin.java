package li.cil.oc2.fabric.mixin.client;

import li.cil.oc2.client.renderer.projector.ProjectorDepthRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides name tags while the projector depth pass renders the level (NeoForge: {@code RenderNameTagEvent}). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void oc2r$noNameTagForProjectorDepth(final CallbackInfo ci) {
        if (ProjectorDepthRenderer.isRenderingProjectorDepth) {
            ci.cancel();
        }
    }
}
