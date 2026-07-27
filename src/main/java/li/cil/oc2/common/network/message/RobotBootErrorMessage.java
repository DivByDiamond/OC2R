package li.cil.oc2.common.network.message;

import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RobotBootErrorMessage(int entityId, @Nullable Component value)
        implements AbstractMessage {
    public static final StreamCodec<RegistryFriendlyByteBuf, RobotBootErrorMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RobotBootErrorMessage::entityId,
                    ComponentSerialization.STREAM_CODEC,
                    RobotBootErrorMessage::value,
                    RobotBootErrorMessage::new);

    public static final CustomPacketPayload.Type<RobotBootErrorMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot_boot_error_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RobotBootErrorMessage(final Robot robot, @Nullable final Component value) {
        this(robot.getId(), value);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withClientEntity(
                entityId,
                Robot.class,
                robot -> robot.getVirtualMachine().setBootErrorClient(value));
    }
}