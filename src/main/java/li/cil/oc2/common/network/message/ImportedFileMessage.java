package li.cil.oc2.common.network.message;

import io.netty.buffer.ByteBuf;

import li.cil.oc2.api.API;
import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ImportedFileMessage(int id, String name, byte[] data) implements AbstractMessage {
    private static final int MAX_NAME_LENGTH = 256;

    public static final StreamCodec<ByteBuf, ImportedFileMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    ImportedFileMessage::id,
                    ByteBufCodecs.STRING_UTF8,
                    ImportedFileMessage::name,
                    ByteBufCodecs.BYTE_ARRAY,
                    ImportedFileMessage::data,
                    ImportedFileMessage::new);

    public static final CustomPacketPayload.Type<ImportedFileMessage> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "imported_file_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleMessage(IPayloadContext context) {
        FileImportExportCardItemDevice.setImportedFile(id, name, data);
    }
}
