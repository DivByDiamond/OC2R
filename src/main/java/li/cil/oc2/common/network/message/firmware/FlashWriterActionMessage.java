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

public record FlashWriterActionMessage(BlockPos pos, UUID session, String action, int length, String name, String digest) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, FlashWriterActionMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FlashWriterActionMessage::pos,
            UUIDUtil.STREAM_CODEC, FlashWriterActionMessage::session,
            ByteBufCodecs.stringUtf8(16), FlashWriterActionMessage::action,
            ByteBufCodecs.VAR_INT, FlashWriterActionMessage::length,
            ByteBufCodecs.stringUtf8(255), FlashWriterActionMessage::name,
            ByteBufCodecs.stringUtf8(64), FlashWriterActionMessage::digest,
            FlashWriterActionMessage::new);
    public static final CustomPacketPayload.Type<FlashWriterActionMessage> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "flash_writer_action_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(final MessageContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                li.cil.oc2.common.firmware.FlashWriterService.action(player, this);
            }
        });
    }
}
