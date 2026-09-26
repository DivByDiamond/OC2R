package li.cil.oc2.client.hooks;

import static li.cil.oc2.common.util.text.TranslationUtils.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import li.cil.oc2.client.gui.screen.file.FileChooserCallback;
import li.cil.oc2.client.gui.screen.file.FileChooserScreen;
import li.cil.oc2.common.bus.device.rpc.item.card.FileImportExportCardItemDevice;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.file.ImportedFileMessage;
import li.cil.oc2.common.network.message.file.cancel.ClientCanceledImportFileMessage;
import li.cil.oc2.common.network.message.misc.MultipartMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Client-side entry points for file import/export network messages. */
@OnlyIn(Dist.CLIENT)
public final class FileTransferHooks {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final MutableComponent FILE_TOO_LARGE_TEXT =
            text("message.{mod}.import_file.file_too_large");

    private FileTransferHooks() {}

    public static void saveExportedFile(final String name, final byte[] data) {
        FileChooserScreen.openFileChooserForSave(
                name,
                path -> {
                    try {
                        Files.write(path, data);
                    } catch (final IOException e) {
                        LOGGER.error(e);
                    }
                });
    }

    public static void requestImportedFile(final int id) {
        FileChooserScreen.openFileChooserForLoad(
                new FileChooserCallback() {
                    @Override
                    public void onFileSelected(final Path path) {
                        try {
                            final Path fileNamePath = path.getFileName();
                            if (fileNamePath == null) {
                                LOGGER.error("Selected path has no file name: {}", path);
                                NetworkMessages.sendToServer(new ClientCanceledImportFileMessage(id));
                                return;
                            }
                            final String fileName = fileNamePath.toString();
                            final byte[] data = Files.readAllBytes(path);
                            if (data.length > FileImportExportCardItemDevice.MAX_TRANSFERRED_FILE_SIZE) {
                                NetworkMessages.sendToServer(new ClientCanceledImportFileMessage(id));
                                Minecraft.getInstance()
                                        .gui
                                        .getChat()
                                        .addMessage(FILE_TOO_LARGE_TEXT.withStyle(
                                                s -> s.withColor(TextColor.fromRgb(0xFFA0A0))));
                            } else {
                                MultipartMessage.sendToServer(new ImportedFileMessage(id, fileName, data));
                            }
                        } catch (final IOException e) {
                            LOGGER.error(e);
                        }
                    }

                    @Override
                    public void onCanceled() {
                        NetworkMessages.sendToServer(new ClientCanceledImportFileMessage(id));
                    }
                });
    }

    public static void closeFileChooser() {
        Minecraft.getInstance().tell(() -> {
            if (Minecraft.getInstance().screen instanceof FileChooserScreen screen) {
                screen.onClose();
            }
        });
    }
}
