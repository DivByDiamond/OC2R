package li.cil.oc2.common.network.message.file;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import li.cil.oc2.client.hooks.FileTransferHooks;
import li.cil.oc2.common.network.message.misc.AbstractMessage;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExportedFileMessage(String name, byte[] data) implements AbstractMessage {
    public static final StreamCodec<ByteBuf, ExportedFileMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ExportedFileMessage::name,
                    ByteBufCodecs.BYTE_ARRAY,
                    ExportedFileMessage::data,
                    ExportedFileMessage::new);

    public static final CustomPacketPayload.Type<ExportedFileMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "exported_file_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleMessage(IPayloadContext context) {
        FileTransferHooks.saveExportedFile(name, data);
    }
}
