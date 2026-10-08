package li.cil.oc2.platform;

import java.util.function.BiConsumer;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
//? if >=26.1 {
/*
*///?} else {
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
//?}
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** {@link MessageRegistrar} wrapping NeoForge's {@link PayloadRegistrar} for one setup event. */
public final class NeoForgeMessageRegistrar implements MessageRegistrar {
    private final PayloadRegistrar registrar;

    public NeoForgeMessageRegistrar(final PayloadRegistrar registrar) {
        this.registrar = registrar;
    }

    private static MessageContext wrap(final IPayloadContext context) {
        return new MessageContext() {
            @Override
            public Player player() {
                return context.player();
            }

            @Override
            public Connection connection() {
                return context.connection();
            }

            @Override
            @SuppressWarnings("FutureReturnValueIgnored")
            public void enqueueWork(final Runnable task) {
                context.enqueueWork(task);
            }
        };
    }

    @Override
    public <T extends CustomPacketPayload> void registerServerbound(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final BiConsumer<T, MessageContext> handler) {
        registrar.playToServer(type, codec, (message, context) -> handler.accept(message, wrap(context)));
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientbound(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final BiConsumer<T, MessageContext> handler) {
        registrar.playToClient(type, codec, (message, context) -> handler.accept(message, wrap(context)));
    }

    @Override
    public <T extends CustomPacketPayload> void registerBidirectional(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final BiConsumer<T, MessageContext> serverboundHandler,
            final BiConsumer<T, MessageContext> clientboundHandler) {
        //? if >=26.1 {
        /*registrar.playBidirectional(
                type,
                codec,
                (message, context) -> serverboundHandler.accept(message, wrap(context)),
                (message, context) -> clientboundHandler.accept(message, wrap(context)));
        *///?} else {
        registrar.playBidirectional(type, codec, new DirectionalPayloadHandler<>(
                (message, context) -> serverboundHandler.accept(message, wrap(context)),
                (message, context) -> clientboundHandler.accept(message, wrap(context))));
        //?}
    }
}
