package li.cil.oc2.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NetworkMessages {
    private NetworkMessages() {
    }

    public static void sendToServer(final CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    public static void sendToClient(final CustomPacketPayload message, final ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }

    public static void sendToClientsTrackingChunk(
            final CustomPacketPayload message, final LevelChunk chunk) {
        // Chunk#getLevel can return something that is not a ServerLevel — most
        // notably on Valkyrien Skies ship worlds used by Create: Aeronautics
        // contraptions. The unconditional cast used to throw ClassCastException
        // out of the VM runner thread, which froze the computer in an "appears
        // on, UART never updates" state. Bail out cleanly instead.
        if (chunk.getLevel() instanceof final ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunk.getPos(), message);
            return;
        }

        // Fallback for non-ServerLevel worlds (e.g. VS2 ship worlds): the
        // regular chunk-tracking API doesn't apply, so broadcast the message
        // to every player who currently has the host level loaded. This is
        // less efficient than chunk-tracking (we send to everyone, not just
        // to players who can see this chunk), but it's the best we can do
        // without a hard dependency on Valkyrien Skies. Players on the same
        // ship — or near it in the parent level — will still get the
        // terminal output and the block face will render correctly.
        final Level hostLevel = chunk.getLevel();
        if (hostLevel == null) {
            return;
        }
        final MinecraftServer server = hostLevel.getServer();
        if (server == null) {
            return;
        }
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Only send to players who actually have a client level — avoids
            // trying to serialize the payload for players in the wrong
            // dimension or in the middle of disconnect/login.
            if (player.connection == null || !player.connection.isAcceptingMessages()) {
                continue;
            }
            PacketDistributor.sendToPlayer(player, message);
        }
    }

    public static void sendToClientsTrackingBlockEntity(
            final CustomPacketPayload message, final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        final MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        if (!server.isSameThread()) {
            throw new IllegalStateException(
                    "Attempting to send network message to BlockEntity from non-server "
                            + "thread ["
                            + Thread.currentThread()
                            + "]. This is not supported, "
                            + "because looking up the chunk from the level is required. "
                            + "Consider caching the containing chunk and using "
                            + "sendToClientsTrackingChunk() directly, instead.");
        }

        final BlockPos blockPos = blockEntity.getBlockPos();
        final int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
        final int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        if (level.hasChunk(chunkX, chunkZ)) {
            final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            sendToClientsTrackingChunk(message, chunk);
        }
    }

    public static void sendToClientsTrackingEntity(
            final CustomPacketPayload message, final Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, message);
    }
}
