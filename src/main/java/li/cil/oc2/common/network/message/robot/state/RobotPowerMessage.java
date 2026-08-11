package li.cil.oc2.common.network.message.robot.state;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RobotPowerMessage(int entityId, boolean power) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, RobotPowerMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RobotPowerMessage::entityId,
                    ByteBufCodecs.BOOL,
                    RobotPowerMessage::power,
                    RobotPowerMessage::new);

    public static final CustomPacketPayload.Type<RobotPowerMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot_power_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RobotPowerMessage(final Robot robot, final boolean power) {
        this(robot.getId(), power);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerEntity(
                context,
                entityId,
                Robot.class,
                robot -> {
                    if (power) {
                        robot.start();
                    } else {
                        robot.stop();
                    }
                });
    }
}