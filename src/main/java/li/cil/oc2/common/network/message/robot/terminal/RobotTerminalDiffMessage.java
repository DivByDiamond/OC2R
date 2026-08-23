package li.cil.oc2.common.network.message.robot.terminal;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import li.cil.oc2.common.vm.terminal.TerminalDiff;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative terminal screen diff for a robot (see {@link TerminalDiff}). */
public record RobotTerminalDiffMessage(int entityId, TerminalDiff.Snapshot snapshot)
        implements AbstractMessage {
    public static final StreamCodec<ByteBuf, RobotTerminalDiffMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RobotTerminalDiffMessage::entityId,
                    TerminalDiff.STREAM_CODEC,
                    RobotTerminalDiffMessage::snapshot,
                    RobotTerminalDiffMessage::new);

    public static final CustomPacketPayload.Type<RobotTerminalDiffMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "robot_terminal_diff_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RobotTerminalDiffMessage(final Robot robot, final TerminalDiff.Snapshot snapshot) {
        this(robot.getId(), snapshot);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientEntity(
                entityId,
                Robot.class,
                robot -> TerminalDiff.apply(robot.getTerminal(), snapshot));
    }
}
