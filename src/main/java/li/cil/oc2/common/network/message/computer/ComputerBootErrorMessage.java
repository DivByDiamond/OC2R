package li.cil.oc2.common.network.message.computer;

import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.common.network.util.ClientBlockEntityLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ComputerBootErrorMessage(BlockPos pos, @Nullable Component value)
        implements AbstractMessage {

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerBootErrorMessage>
            STREAM_CODEC =
                    StreamCodec.composite(
                            BlockPos.STREAM_CODEC,
                            ComputerBootErrorMessage::pos,
                            ComponentSerialization.STREAM_CODEC,
                            ComputerBootErrorMessage::value,
                            ComputerBootErrorMessage::new);

    public static final CustomPacketPayload.Type<ComputerBootErrorMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "computer_boot_error_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public ComputerBootErrorMessage(ComputerBlockEntity computer, @Nullable Component value) {
        this(computer.getBlockPos(), value);
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        ClientBlockEntityLookup.withClientBlockEntityAt(
                pos,
                ComputerBlockEntity.class,
                computer -> computer.getVirtualMachine().setBootErrorClient(value));
    }
}