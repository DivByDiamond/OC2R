package li.cil.oc2.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/** {@link NetworkBridge} backed by the Fabric networking API. */
public final class FabricNetworkBridge implements NetworkBridge {
    @Override
    public void sendToServer(final CustomPacketPayload message) {
        ClientPlayNetworking.send(message);
    }

    @Override
    public void sendToPlayer(final CustomPacketPayload message, final ServerPlayer player) {
        ServerPlayNetworking.send(player, message);
    }

    @Override
    public void sendToPlayersTrackingChunk(
            final CustomPacketPayload message, final ServerLevel level, final ChunkPos pos) {
        for (final ServerPlayer player : PlayerLookup.tracking(level, pos)) {
            ServerPlayNetworking.send(player, message);
        }
    }

    @Override
    public void sendToPlayersTrackingEntity(final CustomPacketPayload message, final Entity entity) {
        for (final ServerPlayer player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, message);
        }
    }
}
