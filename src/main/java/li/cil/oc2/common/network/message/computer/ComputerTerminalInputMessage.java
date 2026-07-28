package li.cil.oc2.common.network.message.computer;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ComputerTerminalInputMessage(BlockPos pos, byte[] data) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ComputerTerminalInputMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ComputerTerminalInputMessage::pos,
                    ByteBufCodecs.BYTE_ARRAY,
                    ComputerTerminalInputMessage::data,
                    ComputerTerminalInputMessage::new);

    public static final CustomPacketPayload.Type<ComputerTerminalInputMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "computer_terminal_input_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ComputerTerminalInputMessage(final ComputerBlockEntity computer, final ByteBuffer data) {
        this(computer.getBlockPos(), data.array());
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(
                context,
                pos,
                ComputerBlockEntity.class,
                (player, computer) -> computer.terminalManager.getTerminal().io.putInput(ByteBuffer.wrap(data)));
    }
}