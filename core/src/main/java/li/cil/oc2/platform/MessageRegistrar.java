package li.cil.oc2.platform;

import java.util.function.BiConsumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Loader-independent registrar for custom network messages, valid only for the duration of loader
 * setup (NeoForge hands out its {@code PayloadRegistrar} through a one-shot setup event, so unlike
 * {@link NetworkBridge} this cannot be a {@link java.util.ServiceLoader}-discovered singleton). Loader
 * setup code constructs the implementation and passes it to {@code Network.initialize} directly.
 */
public interface MessageRegistrar {
    /** Registers a message sent from the client to the server. */
    <T extends CustomPacketPayload> void registerServerbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, MessageContext> handler);

    /** Registers a message sent from the server to the client. */
    <T extends CustomPacketPayload> void registerClientbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, MessageContext> handler);

    /** Registers a message that can travel in either direction, with a handler for each. */
    <T extends CustomPacketPayload> void registerBidirectional(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, MessageContext> serverboundHandler,
            BiConsumer<T, MessageContext> clientboundHandler);
}
