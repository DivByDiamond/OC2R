package li.cil.oc2.fabric.mixin.client;

import java.util.function.Consumer;
import li.cil.oc2.common.blockentity.PlatformBlockEntity;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * NeoForge hands the block entity data of chunk packets to {@code BlockEntity#handleUpdateTag}; vanilla
 * loads it directly. Block entities of this mod get the NeoForge behaviour, all others stay vanilla.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkClientMixin {
    @Shadow
    @Final
    private Level level;

    @Shadow
    public abstract BlockEntity getBlockEntity(net.minecraft.core.BlockPos pos, LevelChunk.EntityCreationType type);

    @ModifyVariable(method = "replaceWithPacketData", at = @At("HEAD"), argsOnly = true)
    private Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> oc2r$handleUpdateTags(
            final Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> original) {
        return vanillaOutput -> original.accept((pos, type, tag) -> {
            final BlockEntity blockEntity = getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
            if (blockEntity instanceof final PlatformBlockEntity platform
                    && tag != null
                    && blockEntity.getType() == type) {
                platform.handleUpdateTag(tag, level.registryAccess());
            } else {
                vanillaOutput.accept(pos, type, tag);
            }
        });
    }
}
