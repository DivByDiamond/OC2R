package li.cil.oc2.common.network.message;

import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RobotRunStateMessage(int entityId, VMRunState value) implements AbstractMessage {
    public static final StreamCodec<FriendlyByteBuf, RobotRunStateMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RobotRunStateMessage::entityId,
                    NeoForgeStreamCodecs.enumCodec(VMRunState.class),
                    RobotRunStateMessage::value,
                    RobotRunStateMessage::new);

    public static final CustomPacketPayload.Type<RobotRunStateMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot_run_state_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RobotRunStateMessage(final Robot robot, final VMRunState value) {
        this(robot.getId(), value);
    }

    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientEntity(
                entityId, Robot.class, robot -> robot.getVirtualMachine().setRunStateClient(value));
    }
}