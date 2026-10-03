package li.cil.oc2.fabric.mixin;

import li.cil.oc2.fabric.FabricInteractionHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** NeoForge's {@code Item.doesSneakBypassUse}: sneaking with such an item still interacts with the block. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Redirect(
            method = "useItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z"))
    private boolean oc2r$sneakBypassUse(final ServerPlayer player) {
        return FabricInteractionHooks.sneakSkipsBlockUse(player, player.isSecondaryUseActive());
    }
}
