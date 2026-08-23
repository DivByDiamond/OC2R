package li.cil.oc2.common.network.message.computer.terminal;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import li.cil.oc2.common.vm.terminal.TerminalDiff;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative terminal screen diff for a computer (see {@link TerminalDiff}). */
public record ComputerTerminalDiffMessage(BlockPos pos, TerminalDiff.Snapshot snapshot)
        implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ComputerTerminalDiffMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ComputerTerminalDiffMessage::pos,
                    TerminalDiff.STREAM_CODEC,
                    ComputerTerminalDiffMessage::snapshot,
                    ComputerTerminalDiffMessage::new);

    public static final CustomPacketPayload.Type<ComputerTerminalDiffMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "computer_terminal_diff_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ComputerTerminalDiffMessage(
            final ComputerBlockEntity computer, final TerminalDiff.Snapshot snapshot) {
        this(computer.getBlockPos(), snapshot);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos,
                ComputerBlockEntity.class,
                computer -> TerminalDiff.apply(computer.terminalManager.getTerminal(), snapshot));
    }
}
