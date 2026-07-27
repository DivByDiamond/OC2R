package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RobotTerminalOutputMessage(int entityId, byte[] data) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, RobotTerminalOutputMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RobotTerminalOutputMessage::entityId,
                    ByteBufCodecs.BYTE_ARRAY,
                    RobotTerminalOutputMessage::data,
                    RobotTerminalOutputMessage::new);

    public static final CustomPacketPayload.Type<RobotTerminalOutputMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "robot_terminal_output_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RobotTerminalOutputMessage(final Robot robot, final ByteBuffer data) {
        this(robot.getId(), data.array());
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientEntity(
                entityId,
                Robot.class,
                robot -> robot.getTerminal().putOutput(ByteBuffer.wrap(data)));
    }
}