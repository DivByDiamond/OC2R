package li.cil.oc2.fabric.mixin;

import li.cil.oc2.common.entity.Robot;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
}
