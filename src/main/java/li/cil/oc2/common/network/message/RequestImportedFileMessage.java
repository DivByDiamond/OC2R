
package li.cil.oc2.common.network.message;

import li.cil.oc2.client.gui.screen.FileChooserCallback;
import li.cil.oc2.client.gui.screen.FileChooserScreen;
import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;
import li.cil.oc2.common.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.MutableComponent;

import io.netty.buffer.ByteBuf;
import li.cil.oc2.api.API;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static li.cil.oc2.common.util.TranslationUtils.text;

public record RequestImportedFileMessage(int id) implements AbstractMessage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final MutableComponent FILE_TOO_LARGE_TEXT = text("message.{mod}.import_file.file_too_large");


    public static final StreamCodec<ByteBuf, RequestImportedFileMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        RequestImportedFileMessage::id,
        RequestImportedFileMessage::new
    );

    public static final CustomPacketPayload.Type<RequestImportedFileMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "request_imported_file_message"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public void handleMessage(IPayloadContext context) {
        FileChooserScreen.openFileChooserForLoad(new FileChooserCallback() {
            @Override
            public void onFileSelected(final Path path) {
                try {
                    final String fileName = path.getFileName().toString();
                    final byte[] data = Files.readAllBytes(path);
                    if (data.length > FileImportExportCardItemDevice.MAX_TRANSFERRED_FILE_SIZE) {
                        Network.sendToServer(new ClientCanceledImportFileMessage(id));
                        Minecraft.getInstance().gui.getChat().addMessage(FILE_TOO_LARGE_TEXT
                            .withStyle(s -> s.withColor(TextColor.fromRgb(0xFFA0A0))));
                    } else {
                        MultipartMessage.sendToServer(new ImportedFileMessage(id, fileName, data));
                    }
                } catch (final IOException e) {
                    LOGGER.error(e);
                }
            }

            @Override
            public void onCanceled() {
                Network.sendToServer(new ClientCanceledImportFileMessage(id));
            }
        });
    }
}
