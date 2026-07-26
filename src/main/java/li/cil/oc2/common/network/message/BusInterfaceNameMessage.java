/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.network.BusCableBlockEntity;
import li.cil.oc2.common.network.ClientBlockEntityLookup;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BusInterfaceNameMessage(BlockPos pos, Direction side, String value) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, BusInterfaceNameMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        BusInterfaceNameMessage::pos,
        Direction.STREAM_CODEC,
        BusInterfaceNameMessage::side,
        ByteBufCodecs.STRING_UTF8,
        BusInterfaceNameMessage::value,
        BusInterfaceNameMessage::new
    );

    public static final CustomPacketPayload.Type<BusInterfaceNameMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "bus_interface_name_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public static BusInterfaceNameMessage ToClient(final BusCableBlockEntity busCable, final Direction side, final String value) {
        return new BusInterfaceNameMessage(busCable.getBlockPos(), side, value);
    }

    public void handleClientMessage(final IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(pos, BusCableBlockEntity.class,
            busCable -> busCable.setInterfaceName(side, value));
    }

    public static BusInterfaceNameMessage ToServer(final BusCableBlockEntity busCable, final Direction side, final String value) {
        return new BusInterfaceNameMessage(busCable.getBlockPos(), side, value);
    }

    public void handleServerMessage(final IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, BusCableBlockEntity.class,
            (player, busCable) -> busCable.setInterfaceName(side, value));
    }
}
