package li.cil.oc2.platform;

import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/** {@link MessageRegistrar} backed by the Fabric networking API. */
public final class FabricMessageRegistrar implements MessageRegistrar {
    private static final boolean CLIENT =
            FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final BiConsumer<T, MessageContext> handler) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        registerServerReceiver(type, handler);
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final BiConsumer<T, MessageContext> handler) {
        PayloadTypeRegistry.playS2C().register(type, codec);
        registerClientReceiver(type, handler);
    }

    @Override
    public <T extends CustomPacketPayload> void registerBidirectional(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final BiConsumer<T, MessageContext> serverboundHandler,
            final BiConsumer<T, MessageContext> clientboundHandler) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        PayloadTypeRegistry.playS2C().register(type, codec);
        registerServerReceiver(type, serverboundHandler);
        registerClientReceiver(type, clientboundHandler);
    }

    private static <T extends CustomPacketPayload> void registerServerReceiver(
            final CustomPacketPayload.Type<T> type, final BiConsumer<T, MessageContext> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                handler.accept(payload, new MessageContext() {
                    @Override
                    public Player player() {
                        return context.player();
                    }

                    @Override
                    public Connection connection() {
                        return context.player().connection.connection;
                    }

                    @Override
                    public void enqueueWork(final Runnable task) {
                        context.server().execute(task);
                    }
                }));
    }

    private static <T extends CustomPacketPayload> void registerClientReceiver(
            final CustomPacketPayload.Type<T> type, final BiConsumer<T, MessageContext> handler) {
        // Receivers go through a separate class so the client-only networking API is never loaded
        // on a dedicated server.
        if (CLIENT) {
            FabricClientReceivers.register(type, handler);
        }
    }
}
