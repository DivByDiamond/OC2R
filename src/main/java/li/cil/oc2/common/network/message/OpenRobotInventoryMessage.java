package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;

import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenRobotInventoryMessage(int entityId) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, OpenRobotInventoryMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    OpenRobotInventoryMessage::entityId,
                    OpenRobotInventoryMessage::new);

    public static final CustomPacketPayload.Type<OpenRobotInventoryMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "open_robot_inventory_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public OpenRobotInventoryMessage(final Robot robot) {
        this(robot.getId());
    }

    public void handleMessage(IPayloadContext context) {
        final ServerPlayer player = (ServerPlayer) context.player();
        MessageUtils.withNearbyServerEntity(
                context, entityId, Robot.class, robot -> robot.openInventoryScreen(player));
    }
}
