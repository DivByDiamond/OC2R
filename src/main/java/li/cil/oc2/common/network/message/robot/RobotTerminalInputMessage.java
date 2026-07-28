package li.cil.oc2.common.network.message.robot;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RobotTerminalInputMessage(int entityId, byte[] data) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, RobotTerminalInputMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RobotTerminalInputMessage::entityId,
                    ByteBufCodecs.BYTE_ARRAY,
                    RobotTerminalInputMessage::data,
                    RobotTerminalInputMessage::new);

    public static final CustomPacketPayload.Type<RobotTerminalInputMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "robot_terminal_input_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RobotTerminalInputMessage(final Robot robot, final ByteBuffer data) {
        this(robot.getId(), data.array());
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerEntity(
                context,
                entityId,
                Robot.class,
                robot -> robot.getTerminal().io.putInput(ByteBuffer.wrap(data)));
    }
}