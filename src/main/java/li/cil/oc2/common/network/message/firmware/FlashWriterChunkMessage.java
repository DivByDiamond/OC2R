package li.cil.oc2.common.network.message.firmware;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification =
                "the payload is a fresh array that the stream codec decoded for this message; the"
                    + " receiver only reads it and never mutates it")
public record FlashWriterChunkMessage(BlockPos pos, UUID session, int offset, byte[] data) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, FlashWriterChunkMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FlashWriterChunkMessage::pos,
            UUIDUtil.STREAM_CODEC, FlashWriterChunkMessage::session,
            ByteBufCodecs.VAR_INT, FlashWriterChunkMessage::offset,
            ByteBufCodecs.byteArray(li.cil.oc2.common.firmware.FlashImageUpload.CHUNK_SIZE), FlashWriterChunkMessage::data,
            FlashWriterChunkMessage::new);
    public static final CustomPacketPayload.Type<FlashWriterChunkMessage> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "flash_writer_chunk_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(final MessageContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                li.cil.oc2.common.firmware.FlashWriterService.chunk(player, this);
            }
        });
    }
}
