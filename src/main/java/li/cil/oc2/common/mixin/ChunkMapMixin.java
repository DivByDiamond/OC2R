package li.cil.oc2.common.mixin;

import li.cil.oc2.common.util.world.chunk.ChunkUtils;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the chunks that are marked for lazy saving before the chunk map saves them.
 *
 * <p>The Fabric module carries a copy of this mixin ({@code li.cil.oc2.fabric.mixin.ChunkMapMixin}).
 * The duplication is not an oversight: the module compiles the shared code of the root project from
 * a separate source set, so the class cannot be reused, and the two mixin configs never co-exist
 * (see docs/roadmap/multiloader.md §42).
 *
 * <p>Both copies inject the same method into the same target. They differ only in the
 * {@code remap} attribute, because the two loaders name the target differently: NeoForge has no
 * SRG name for the private target and remaps nothing, while Loom maps the target to the
 * intermediary name the game runs with.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    // remap = false: the target is a private method and has no SRG name, so there is nothing to
    // remap it to (same reason as ServerChunkCacheMixin).
    @Inject(method = "save", at = @At("HEAD"), remap = false)
    private void applyLazyUnsavedChunk(final ChunkAccess chunk, final CallbackInfoReturnable<Boolean> ignored) {
        ChunkUtils.applyChunkLazyUnsaved(chunk);
    }
}
