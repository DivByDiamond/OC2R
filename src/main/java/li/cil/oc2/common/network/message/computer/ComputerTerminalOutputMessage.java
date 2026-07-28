package li.cil.oc2.common.network.message.computer;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ComputerTerminalOutputMessage(BlockPos pos, byte[] data) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ComputerTerminalOutputMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ComputerTerminalOutputMessage::pos,
                    ByteBufCodecs.BYTE_ARRAY,
                    ComputerTerminalOutputMessage::data,
                    ComputerTerminalOutputMessage::new);

    public static final CustomPacketPayload.Type<ComputerTerminalOutputMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "computer_terminal_output_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ComputerTerminalOutputMessage(
            final ComputerBlockEntity computer, final ByteBuffer data) {
        this(computer.getBlockPos(), data.array());
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos,
                ComputerBlockEntity.class,
                computer -> computer.terminalManager.getTerminal().io.putOutput(ByteBuffer.wrap(data)));
    }
}