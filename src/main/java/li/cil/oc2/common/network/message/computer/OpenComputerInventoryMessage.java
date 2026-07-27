package li.cil.oc2.common.network.message.computer;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.network.util.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import li.cil.oc2.common.network.message.misc.AbstractMessage;

public record OpenComputerInventoryMessage(BlockPos pos) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, OpenComputerInventoryMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenComputerInventoryMessage::pos,
                    OpenComputerInventoryMessage::new);

    public static final CustomPacketPayload.Type<OpenComputerInventoryMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "open_computer_inventory_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public OpenComputerInventoryMessage(final ComputerBlockEntity computer) {
        this(computer.getBlockPos());
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(
                context,
                pos,
                ComputerBlockEntity.class,
                (player, computer) -> computer.terminalManager.openInventoryScreen(player));
    }
}