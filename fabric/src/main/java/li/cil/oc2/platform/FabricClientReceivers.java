package li.cil.oc2.platform;

import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/** Client-side half of {@link FabricMessageRegistrar}; only ever loaded on the physical client. */
@Environment(EnvType.CLIENT)
final class FabricClientReceivers {
    private FabricClientReceivers() {}

    static <T extends CustomPacketPayload> void register(
            final CustomPacketPayload.Type<T> type, final BiConsumer<T, MessageContext> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                handler.accept(payload, new MessageContext() {
                    @Override
                    public Player player() {
                        return context.player();
                    }

                    @Override
                    public Connection connection() {
                        return context.client().getConnection().getConnection();
                    }

                    @Override
                    public void enqueueWork(final Runnable task) {
                        context.client().execute(task);
                    }
                }));
    }
}
