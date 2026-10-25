package li.cil.oc2.fabric.mixin;

//? if >=26.1 {
/*import li.cil.oc2.common.block.cable.BusCableBlock;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.fabric.FabricInteractionHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else {
import li.cil.oc2.common.entity.Robot;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    // The target is remapped like every other injection here (see ChunkMapMixin): Loom's
    // intermediary mappings do carry a name for it (`method_29780`), so the default remapping
    // resolves the selector. The NeoForge counterpart keeps `remap = false`, because that
    // mapping set has no SRG name for the private method.
    @Inject(method = "noBlocksAround", at = @At("RETURN"), cancellable = true)
    private void standingOnRobotIsNotFloating(
            final Entity entity, final CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()
                && !entity.level()
                        .getEntitiesOfClass(
                                Robot.class,
                                entity.getBoundingBox().inflate(0.0625).expandTowards(0, -0.55, 0))
                        .isEmpty()) {
            cir.setReturnValue(false);
        }
    }

    //? if >=26.1 {
    /*@Shadow
    public ServerPlayer player;

    // Modern vanilla resolves pick-block on the server; preserve its include-data behavior for
    // every ordinary block and use the cable's hit-aware selection only for our bus cable.
    @Redirect(method = "handlePickItemFromBlock", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack("
                    + "Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)"
                    + "Lnet/minecraft/world/item/ItemStack;"))
    // NOPMD - Mixin callback names use the mod namespace to avoid collisions with other mods.
    @SuppressWarnings("PMD.AvoidDollarSigns") // Mixin handler names use the mod namespace to avoid collisions.
    private ItemStack oc2r$pickCableInterface(
            final BlockState state, final LevelReader level, final BlockPos pos, final boolean includeData) {
        if (state.getBlock() instanceof BusCableBlock) {
            final var hit = player.pick(player.blockInteractionRange() + 1, 1, false);
            if (hit instanceof final BlockHitResult blockHit && blockHit.getBlockPos().equals(pos)) {
                return FabricInteractionHooks.pickBlock(state, hit, level, pos, player);
            }
        }
        return state.getCloneItemStack(level, pos, includeData);
    }
    *///?}

}
