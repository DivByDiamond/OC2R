package li.cil.oc2.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.1 {
/*import li.cil.oc2.client.renderer.projector.ProjectorPipelineRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

@Mixin(GameRenderer.class)
public abstract class ProjectorProjectionMixin {
    //? if >=26.1 {
/*    // Keep the local matrix while vanilla applies view bob, hurt and portal effects to it.
    // CameraRenderState.projectionMatrix is the unmodified lens and cannot reconstruct that depth.
    @ModifyVariable(method = "renderLevel", at = @At("STORE"), ordinal = 0, remap = false)
    private Matrix4f captureProjection(final Matrix4f projection) {
        ProjectorPipelineRenderer.setMainProjection(projection);
        return projection;
    }
*///?}
}
