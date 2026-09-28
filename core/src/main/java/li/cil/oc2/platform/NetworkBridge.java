package li.cil.oc2.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/**
 * Loader-independent entry point for sending custom network messages.
 *
 * <p>Each mod loader module (NeoForge, later Fabric) provides one stateless implementation, discovered
 * through {@link java.util.ServiceLoader} via {@link Platform#network()}. Message <em>registration</em>
 * is not part of this interface: NeoForge only exposes its {@code PayloadRegistrar} for the duration of
 * a setup event, so it cannot be handed out as a singleton the way this bridge is. See
 * {@link MessageRegistrar} for that half, which loader setup code constructs directly instead.
 */
public interface NetworkBridge {
    /** Sends {@code message} from the client to the server. */
    void sendToServer(CustomPacketPayload message);

    /** Sends {@code message} from the server to a single client. */
    void sendToPlayer(CustomPacketPayload message, ServerPlayer player);

    /** Sends {@code message} to every client currently tracking the chunk at {@code pos} in {@code level}. */
    void sendToPlayersTrackingChunk(CustomPacketPayload message, ServerLevel level, ChunkPos pos);

    /** Sends {@code message} to every client currently tracking {@code entity}. */
    void sendToPlayersTrackingEntity(CustomPacketPayload message, Entity entity);
}
