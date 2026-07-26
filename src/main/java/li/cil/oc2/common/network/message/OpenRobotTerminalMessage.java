/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenRobotTerminalMessage(int entityId) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, OpenRobotTerminalMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        OpenRobotTerminalMessage::entityId,
        OpenRobotTerminalMessage::new
    );

    public static final CustomPacketPayload.Type<OpenRobotTerminalMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "open_robot_terminal_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    ///////////////////////////////////////////////////////////////////

    public OpenRobotTerminalMessage(final Robot robot) {
        this(robot.getId());
    }

    public void handleMessage(IPayloadContext context) {
        final ServerPlayer player = (ServerPlayer) context.player();
        MessageUtils.withNearbyServerEntity(context, entityId, Robot.class,
            robot -> robot.openTerminalScreen(player));
    }
}
