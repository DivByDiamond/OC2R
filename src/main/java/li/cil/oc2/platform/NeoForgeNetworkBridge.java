package li.cil.oc2.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** Stateless {@link NetworkBridge} backed by NeoForge's {@link PacketDistributor}. */
public final class NeoForgeNetworkBridge implements NetworkBridge {
    @Override
    public void sendToServer(final CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    @Override
    public void sendToPlayer(final CustomPacketPayload message, final ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }

    @Override
    public void sendToPlayersTrackingChunk(
            final CustomPacketPayload message, final ServerLevel level, final ChunkPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(level, pos, message);
    }

    @Override
    public void sendToPlayersTrackingEntity(final CustomPacketPayload message, final Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, message);
    }
}
