package li.cil.oc2.fabric.mixin;

import li.cil.oc2.fabric.FabricInteractionHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Pick-block is resolved on the client in 1.21.1 (the server only sees the resulting slot), so the
 * NeoForge overload of {@code Block.getCloneItemStack} with hit result and player is routed here.
 * Registered in the {@code client} section of the mixin config.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftPickBlockMixin {
    @Shadow
    public HitResult hitResult;

    @Shadow
    public LocalPlayer player;

    @Redirect(
            method = "pickBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;getCloneItemStack("
                            + "Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack oc2r$pickBlock(
            final Block block, final LevelReader level, final BlockPos pos, final BlockState state) {
        return FabricInteractionHooks.pickBlock(state, hitResult, level, pos, player);
    }
}
