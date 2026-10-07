package li.cil.oc2.common.network.message.file;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.common.hooks.ClientProxy;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import li.cil.oc2.platform.MessageContext;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestImportedFileMessage(int id) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, RequestImportedFileMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    RequestImportedFileMessage::id,
                    RequestImportedFileMessage::new);

    public static final CustomPacketPayload.Type<RequestImportedFileMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            API.MOD_ID, "request_imported_file_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(MessageContext context) {
        ClientProxy.get().requestImportedFile(id);
    }
}
