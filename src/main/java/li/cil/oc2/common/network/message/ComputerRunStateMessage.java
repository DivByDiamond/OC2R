/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ComputerRunStateMessage(BlockPos pos, VMRunState value) implements AbstractMessage {
    public static final StreamCodec<FriendlyByteBuf, ComputerRunStateMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ComputerRunStateMessage::pos,
        NeoForgeStreamCodecs.enumCodec(VMRunState.class),
        ComputerRunStateMessage::value,
        ComputerRunStateMessage::new
    );

    public static final CustomPacketPayload.Type<ComputerRunStateMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "computer_run_state_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public ComputerRunStateMessage(final ComputerBlockEntity computer, final VMRunState value) {
        this(computer.getBlockPos(), value);
    }

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ComputerBlockEntity.class,
            computer -> computer.getVirtualMachine().setRunStateClient(value));
    }
}
