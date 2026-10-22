package li.cil.oc2.common.network.message.firmware;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FlashWriterStatusMessage(BlockPos pos, UUID session, String state, int offset,
        int capacity, String detail) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, FlashWriterStatusMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FlashWriterStatusMessage::pos,
            UUIDUtil.STREAM_CODEC, FlashWriterStatusMessage::session,
            ByteBufCodecs.stringUtf8(16), FlashWriterStatusMessage::state,
            ByteBufCodecs.VAR_INT, FlashWriterStatusMessage::offset,
            ByteBufCodecs.VAR_INT, FlashWriterStatusMessage::capacity,
            ByteBufCodecs.stringUtf8(255), FlashWriterStatusMessage::detail,
            FlashWriterStatusMessage::new);
    public static final CustomPacketPayload.Type<FlashWriterStatusMessage> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "flash_writer_status_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(final MessageContext context) {
        context.enqueueWork(() -> li.cil.oc2.common.hooks.ClientProxy.get()
                .handleFlashWriterStatus(pos, session, state, offset, capacity, detail));
    }
}
