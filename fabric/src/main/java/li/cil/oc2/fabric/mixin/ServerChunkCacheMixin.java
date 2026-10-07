package li.cil.oc2.fabric.mixin;

import li.cil.oc2.common.util.world.chunk.ChunkUtils;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric counterpart of the NeoForge mixin of the same name: applies lazily unsaved chunks before saving. */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {
    @Inject(method = "save", at = @At("HEAD"))
    private void oc2r$applyLazyUnsavedChunks(final boolean flush, final CallbackInfo ci) {
        ChunkUtils.applyChunkLazyUnsaved();
    }
}
