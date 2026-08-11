package li.cil.oc2.common.network.message.computer.terminal;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenComputerTerminalMessage(BlockPos pos) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, OpenComputerTerminalMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    OpenComputerTerminalMessage::pos,
                    OpenComputerTerminalMessage::new);

    public static final CustomPacketPayload.Type<OpenComputerTerminalMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "open_computer_terminal_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public OpenComputerTerminalMessage(final ComputerBlockEntity computer) {
        this(computer.getBlockPos());
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(
                context,
                pos,
                ComputerBlockEntity.class,
                (player, computer) -> computer.terminalManager.openTerminalScreen(player));
    }
}