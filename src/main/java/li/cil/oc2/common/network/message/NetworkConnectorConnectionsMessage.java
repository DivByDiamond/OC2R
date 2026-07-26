/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.network.NetworkConnectorBlockEntity;
import li.cil.oc2.common.network.ClientBlockEntityLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;

public record NetworkConnectorConnectionsMessage(BlockPos pos, ArrayList<BlockPos> connectedPositions) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, NetworkConnectorConnectionsMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        NetworkConnectorConnectionsMessage::pos,
        ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC),
        NetworkConnectorConnectionsMessage::connectedPositions,
        NetworkConnectorConnectionsMessage::new
    );

    public static final CustomPacketPayload.Type<NetworkConnectorConnectionsMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "network_connector_connections_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public NetworkConnectorConnectionsMessage(final NetworkConnectorBlockEntity networkConnector) {
        this(networkConnector.getBlockPos(), new ArrayList<>(networkConnector.getConnectedPositions()));
    }

    ///////////////////////////////////////////////////////////////////

    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(pos, NetworkConnectorBlockEntity.class,
            networkConnector -> networkConnector.setConnectedPositionsClient(connectedPositions));
    }
}
