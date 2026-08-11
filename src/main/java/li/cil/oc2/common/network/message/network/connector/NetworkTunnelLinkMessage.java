package li.cil.oc2.common.network.message.network.connector;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.container.network.NetworkTunnelContainer;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NetworkTunnelLinkMessage(int containerId) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, NetworkTunnelLinkMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    NetworkTunnelLinkMessage::containerId,
                    NetworkTunnelLinkMessage::new);

    public static final CustomPacketPayload.Type<NetworkTunnelLinkMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "network_tunnel_link_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        final ServerPlayer player = (ServerPlayer) context.player();

        final AbstractContainerMenu container = player.containerMenu;
        if (container.containerId != containerId) {
            return;
        }

        if (container instanceof NetworkTunnelContainer networkTunnelContainer) {
            networkTunnelContainer.createTunnel();
        }
    }
}