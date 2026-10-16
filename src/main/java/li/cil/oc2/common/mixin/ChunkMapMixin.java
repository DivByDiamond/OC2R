package li.cil.oc2.common.mixin;

import li.cil.oc2.common.util.world.chunk.ChunkUtils;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Inject(method = "save", at = @At("HEAD"), remap = false)
    private void applyLazyUnsavedChunk(final ChunkAccess chunk, final CallbackInfoReturnable<Boolean> ignored) {
        ChunkUtils.applyChunkLazyUnsaved(chunk);
    }
}
