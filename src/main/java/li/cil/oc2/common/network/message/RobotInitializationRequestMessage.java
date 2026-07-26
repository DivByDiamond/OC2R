
package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.Network;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RobotInitializationRequestMessage(int entityId) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, RobotInitializationRequestMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        RobotInitializationRequestMessage::entityId,
        RobotInitializationRequestMessage::new
    );

    public static final CustomPacketPayload.Type<RobotInitializationRequestMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot_initialization_request_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public RobotInitializationRequestMessage(final Robot robot) {
        this(robot.getId());
    }

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withServerEntity(context, entityId, Robot.class,
            robot -> {
                var player = (ServerPlayer) context.player();
                Network.sendToClient(new RobotInitializationMessage(robot), player);
        });
    }
}
