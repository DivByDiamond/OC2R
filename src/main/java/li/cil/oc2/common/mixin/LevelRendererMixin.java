package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Mutable
    @Shadow(remap = false)
    @Final
    private RenderBuffers renderBuffers;

    @Shadow(remap = false)
    @Final
    private Minecraft minecraft;

    @Shadow(remap = false)
    private Frustum cullingFrustum;

    @Shadow(remap = false)
    @Nullable
    private RenderTarget itemEntityTarget;

    @Nullable private RenderTarget itemEntityTargetBak;

    @Shadow(remap = false)
    @Nullable
    private RenderTarget weatherTarget;

    @Nullable private RenderTarget weatherTargetBak;

    @Shadow(remap = false)
    protected abstract void renderSnowAndRain(
            final LightTexture lightTexture,
            final float partialTicks,
            final double cameraX,
            final double cameraY,
            final double cameraZ);

    @Shadow(remap = false)
    @Nullable
    private Frustum capturedFrustum;

    @Inject(method = "renderLevel", at = @At("HEAD"), remap = false)
    private void prepareDepthRendering(final CallbackInfo ignored) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            itemEntityTargetBak = itemEntityTarget;
            itemEntityTarget = null;
            weatherTargetBak = weatherTarget;
            weatherTarget = null;
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"), remap = false)
    private void cleanupDepthRendering(final CallbackInfo ignored) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            cleanupDepthRendering();
        }
    }

    private void cleanupDepthRendering() {
        weatherTarget = weatherTargetBak;
        itemEntityTarget = itemEntityTargetBak;
    }

    @Inject(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE_STRING",
                            target =
                                    "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                            args = {"ldc=destroyProgress"}),
            cancellable = true,
            remap = false)
    private void captureDepthAndEarlyExit(
            DeltaTracker deltaTracker,
            boolean ignored,
            Camera camera,
            GameRenderer ignored2,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            // If we're rendering depth, we can skip most of the rest here: we don't need
            // destruction progress,
            // transparency, hit result, debug stuff, clouds.
            cleanupDepthRendering();
            float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);

            // We do want particles and weather (rain) though, because that's a neat effect.
            final MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();
            minecraft.particleEngine.render(
                    lightTexture, camera, partialTicks, cullingFrustum, (ty) -> true);
            bufferSource.endBatch();

            final Vec3 cameraPosition = camera.getPosition();
            renderSnowAndRain(
                    lightTexture,
                    partialTicks,
                    cameraPosition.x(),
                    cameraPosition.y(),
                    cameraPosition.z());

            // Clean up anything regular return would also clean up.
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
            FogRenderer.setupNoFog();
            ci.cancel();
        } else if (ProjectorDepthRenderer.willRenderProjectorDepth()) {
            // Flush buffers that may write to depth buffer, e.g. when rendering item stacks.
            final MultiBufferSource.BufferSource bufferSource = renderBuffers.bufferSource();
            bufferSource.endBatch(Sheets.translucentCullBlockSheet());
            bufferSource.endBatch(Sheets.bannerSheet());
            bufferSource.endBatch(Sheets.shieldSheet());

            // Otherwise, we grab the depth buffer of the main render target here, before
            // fabulous shading breaks it.
            ProjectorDepthRenderer.captureMainCameraDepth();
        }

        cullingFrustum = new Frustum(frustumMatrix, projectionMatrix);
    }

    /**
     * Make sure weather effects are rendered with depth, so they cause "shadows" in our projection.
     */
    @Inject(
            method = "renderSnowAndRain",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V",
                            shift = At.Shift.AFTER),
            remap = false)
    private void enableDepthForWeatherInDepthBuffer(final CallbackInfo ignored) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            RenderSystem.depthMask(true);
        }
    }

    /** Don't render outlines while rendering projector depth. */
    @Inject(
            method = "shouldShowEntityOutlines",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void skipOutlines(final CallbackInfoReturnable<Boolean> cir) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            cir.setReturnValue(false);
        }
    }

    /** Skip rendering the sky when rendering projector depth. */
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true, remap = false)
    private void skipSky(final CallbackInfo ci) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"entityTarget", "getItemEntityTarget", "getWeatherTarget"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void redirectToMainTarget(final CallbackInfoReturnable<RenderTarget> cir) {
        if (ProjectorDepthRenderer.isIsRenderingProjectorDepth()) {
            cir.setReturnValue(Minecraft.getInstance().getMainRenderTarget());
        }
    }
}