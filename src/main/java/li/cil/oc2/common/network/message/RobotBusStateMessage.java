package li.cil.oc2.common.network.message;

import li.cil.oc2.api.API;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RobotBusStateMessage(int entityId, BusState value) implements AbstractMessage {
    public static final StreamCodec<FriendlyByteBuf, RobotBusStateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RobotBusStateMessage::entityId,
                    NeoForgeStreamCodecs.enumCodec(BusState.class),
                    RobotBusStateMessage::value,
                    RobotBusStateMessage::new);

    public static final CustomPacketPayload.Type<RobotBusStateMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot_bus_state_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RobotBusStateMessage(final Robot robot, final BusState value) {
        this(robot.getId(), value);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientEntity(
                entityId, Robot.class, robot -> robot.getVirtualMachine().setBusStateClient(value));
    }
}