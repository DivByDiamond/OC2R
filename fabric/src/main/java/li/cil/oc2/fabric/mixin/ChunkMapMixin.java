package li.cil.oc2.fabric.mixin;

import li.cil.oc2.common.util.world.chunk.ChunkUtils;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric counterpart of the NeoForge {@code li.cil.oc2.common.mixin.ChunkMapMixin}: applies the
 * chunks that are marked for lazy saving before the chunk map saves them.
 *
 * <p>The implementation is duplicated on purpose: the module compiles the shared code of the root
 * project from a separate source set, so the NeoForge class cannot be reused, and the two mixin
 * configs never co-exist (see docs/roadmap/multiloader.md §42).
 *
 * <p>Both copies inject the same method into the same target. They differ only in the
 * {@code remap} attribute, because the two loaders name the target differently: Loom maps the
 * target to the intermediary name the game runs with, while NeoForge has no SRG name for the
 * private target and remaps nothing.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    // The remap is left at its default (true): Loom rewrites the target to the intermediary name.
    // The NeoForge copy sets remap = false, because that loader has no SRG name for the private
    // target and remaps nothing at all.
    @Inject(method = "save", at = @At("HEAD"))
    private void applyLazyUnsavedChunk(final ChunkAccess chunk, final CallbackInfoReturnable<Boolean> ignored) {
        ChunkUtils.applyChunkLazyUnsaved(chunk);
    }
}
