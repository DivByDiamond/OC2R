package li.cil.oc2.fabric.mixin;

import li.cil.oc2.platform.FabricCapabilityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no block-state-change event, and NeoForge invalidates capabilities whenever a block
 * changes. This reproduces that so cached neighbor lookups and listeners refresh.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void oc2r$invalidateCapabilities(
            final BlockPos pos,
            final BlockState state,
            final boolean moved,
            final CallbackInfoReturnable<BlockState> cir) {
        // A null return value means nothing changed.
        if (cir.getReturnValue() != null) {
            FabricCapabilityBridge.invalidate(((LevelChunk) (Object) this).getLevel(), pos);
        }
    }
}
