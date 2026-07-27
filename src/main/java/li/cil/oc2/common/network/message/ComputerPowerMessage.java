package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ComputerPowerMessage(BlockPos pos, boolean power) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ComputerPowerMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ComputerPowerMessage::pos,
                    ByteBufCodecs.BOOL,
                    ComputerPowerMessage::power,
                    ComputerPowerMessage::new);

    public static final CustomPacketPayload.Type<ComputerPowerMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "computer_power_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ComputerPowerMessage(final ComputerBlockEntity computer, final boolean power) {
        this(computer.getBlockPos(), power);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(
                context,
                pos,
                ComputerBlockEntity.class,
                (player, computer) -> {
                    if (power) {
                        computer.start();
                    } else {
                        computer.stop();
                    }
                });
    }
}