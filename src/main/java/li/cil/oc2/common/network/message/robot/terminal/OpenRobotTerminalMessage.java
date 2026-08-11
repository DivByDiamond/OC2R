package li.cil.oc2.common.network.message.robot.terminal;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.MessageUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenRobotTerminalMessage(int entityId) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, OpenRobotTerminalMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    OpenRobotTerminalMessage::entityId,
                    OpenRobotTerminalMessage::new);

    public static final CustomPacketPayload.Type<OpenRobotTerminalMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "open_robot_terminal_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public OpenRobotTerminalMessage(final Robot robot) {
        this(robot.getId());
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        final ServerPlayer player = (ServerPlayer) context.player();
        MessageUtils.withNearbyServerEntity(
                context, entityId, Robot.class, robot -> robot.openTerminalScreen(player));
    }
}