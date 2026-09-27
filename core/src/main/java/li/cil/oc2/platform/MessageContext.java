package li.cil.oc2.platform;

import net.minecraft.network.Connection;
import net.minecraft.world.entity.player.Player;

/**
 * Loader-independent view of the context a network message is handled in, replacing NeoForge's
 * {@code IPayloadContext}. Covers the three accessors message handlers across the mod actually use.
 */
public interface MessageContext {
    /** The player sending (serverbound) or receiving (clientbound) the message. */
    Player player();

    /** The connection the message arrived on, usable as an identity key (see multipart reassembly). */
    Connection connection();

    /** Schedules {@code task} to run on the main thread of the side handling the message. */
    void enqueueWork(Runnable task);
}
