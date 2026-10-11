package li.cil.oc2.common.network.message.robot.state;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import li.cil.oc2.common.vm.PowerAction;

public record RobotPowerMessage(int entityId, PowerAction action) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, RobotPowerMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RobotPowerMessage::entityId,
                    PowerAction.STREAM_CODEC,
                    RobotPowerMessage::action,
                    RobotPowerMessage::new);

    public static final CustomPacketPayload.Type<RobotPowerMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot_power_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RobotPowerMessage(final Robot robot, final PowerAction action) {
        this(robot.getId(), action);
    }

    @Override
    public void handleMessage(MessageContext context) {
        MessageUtils.withNearbyServerEntity(
                context,
                entityId,
                Robot.class,
                robot -> {
                    switch (action) {
                        case START -> robot.start();
                        case SHUTDOWN -> robot.shutdown();
                        case REBOOT -> robot.reboot();
                        case POWER_OFF -> robot.stop();
                    }
                });
    }
}