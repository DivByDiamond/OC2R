/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ComputerBusStateMessage(BlockPos pos, CommonDeviceBusController.BusState value) implements AbstractMessage {
    public static final StreamCodec<FriendlyByteBuf, ComputerBusStateMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ComputerBusStateMessage::pos,
        NeoForgeStreamCodecs.enumCodec(CommonDeviceBusController.BusState.class),
        ComputerBusStateMessage::value,
        ComputerBusStateMessage::new
    );

    public static final CustomPacketPayload.Type<ComputerBusStateMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "computer_bus_state_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public ComputerBusStateMessage(final ComputerBlockEntity computer, final CommonDeviceBusController.BusState value) {
        this(computer.getBlockPos(), value);
    }

    public void handleMessage(final IPayloadContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ComputerBlockEntity.class,
            computer -> computer.getVirtualMachine().setBusStateClient(value));
    }
}
