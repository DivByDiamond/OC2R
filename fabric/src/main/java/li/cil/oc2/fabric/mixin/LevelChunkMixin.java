package li.cil.oc2.fabric.mixin;

import li.cil.oc2.common.blockentity.PlatformBlockEntity;
import li.cil.oc2.platform.FabricCapabilityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no block-state-change event, and NeoForge invalidates capabilities whenever a block
 * changes. This reproduces that so cached neighbor lookups and listeners refresh. It also reproduces
 * NeoForge's {@code BlockEntity#onChunkUnloaded} call.
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

    // NeoForge calls onChunkUnloaded on every block entity first thing in clearAllBlockEntities.
    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void oc2r$notifyChunkUnloaded(final CallbackInfo ci) {
        for (final BlockEntity blockEntity : ((LevelChunk) (Object) this).getBlockEntities().values()) {
            if (blockEntity instanceof final PlatformBlockEntity platform) {
                platform.onChunkUnloaded();
            }
        }
    }
}
