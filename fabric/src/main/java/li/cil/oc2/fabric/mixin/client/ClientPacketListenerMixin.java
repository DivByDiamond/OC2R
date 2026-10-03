package li.cil.oc2.fabric.mixin.client;

import li.cil.oc2.common.blockentity.PlatformBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge hands block entity data packets to {@code BlockEntity#onDataPacket}; vanilla loads the tag
 * directly. Block entities of this mod get the NeoForge behaviour, all others stay vanilla.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private RegistryAccess.Frozen registryAccess;

    @Inject(
            method = "handleBlockEntityData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread("
                            + "Lnet/minecraft/network/protocol/Packet;"
                            + "Lnet/minecraft/network/PacketListener;"
                            + "Lnet/minecraft/util/thread/BlockableEventLoop;)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void oc2r$onDataPacket(final ClientboundBlockEntityDataPacket packet, final CallbackInfo ci) {
        if (minecraft.level == null) {
            return;
        }
        final BlockEntity blockEntity = minecraft.level.getBlockEntity(packet.getPos());
        if (blockEntity instanceof final PlatformBlockEntity platform && blockEntity.getType() == packet.getType()) {
            platform.onDataPacket(minecraft.getConnection().getConnection(), packet, registryAccess);
            ci.cancel();
        }
    }
}
