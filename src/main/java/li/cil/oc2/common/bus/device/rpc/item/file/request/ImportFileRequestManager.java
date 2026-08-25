package li.cil.oc2.common.bus.device.rpc.item.file.request;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import li.cil.oc2.common.bus.device.rpc.item.card.FileImportExportCardItemDevice;
import li.cil.oc2.common.bus.device.rpc.item.file.ImportExportState;
import li.cil.oc2.common.bus.device.rpc.item.file.ImportedFile;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.file.cancel.ServerCanceledImportFileMessage;
import net.minecraft.server.level.ServerPlayer;

public class ImportFileRequestManager {

    private static final ReentrantLock lock = new ReentrantLock();

    private static final Int2ObjectMap<ImportFileRequest> importingDevices =
            new Int2ObjectArrayMap<>();
    private static int nextImportId = 1;

    public static int registerRequest(final FileImportExportCardItemDevice device) {
        lock.lock();
        try {
            // Incremented under the lock: registerRequest runs on VM threads while
            // network handlers run on the server thread; a torn increment handed out
            // duplicate ids and silently dropped one of the requests.
            final int id = nextImportId++;
            importingDevices.put(id, new ImportFileRequest(device));
            return id;
        } finally {
            lock.unlock();
        }
    }

    public static void removeRequest(final int id) {
        lock.lock();
        try {
            importingDevices.remove(id);
        } finally {
            lock.unlock();
        }
    }

    public static void setImportedFile(
            @Nullable final ServerPlayer sender,
            final int id,
            final String name,
            final byte[] data) {
        if (data.length > FileImportExportCardItemDevice.MAX_TRANSFERRED_FILE_SIZE) {
            return;
        }
        final String safeName = sanitizeFileName(name);
        if (safeName == null) {
            return;
        }
        lock.lock();
        try {
            final ImportFileRequest request = importingDevices.get(id);
            // Only players the import was offered to may deliver the file; do not
            // consume the request otherwise so an attacker cannot cancel someone
            // else's import with a forged message.
            if (request == null || sender == null || !request.PendingPlayers.contains(sender)) {
                return;
            }
            importingDevices.remove(id);
            final FileImportExportCardItemDevice device = request.Device.get();
            if (device != null) {
                device.importedFile = new ImportedFile(safeName, data);
                final ServerCanceledImportFileMessage message =
                        new ServerCanceledImportFileMessage(id);
                for (final ServerPlayer serverPlayer : request.PendingPlayers) {
                    NetworkMessages.sendToClient(message, serverPlayer);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public static void cancelImport(final ServerPlayer player, final int id) {
        lock.lock();
        try {
            final ImportFileRequest request = importingDevices.get(id);
            if (request == null) {
                return;
            }
            request.PendingPlayers.remove(player);
            if (!request.PendingPlayers.isEmpty()) {
                return;
            }
            importingDevices.remove(id);
            final FileImportExportCardItemDevice device = request.Device.get();
            if (device != null) {
                device.state = ImportExportState.IMPORT_CANCELED;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reduces a client-supplied file name to a safe bare base name: strips any path
     * components (for both separator styles), trims surrounding whitespace, and
     * rejects empty names, names longer than 255 characters, and names containing
     * control characters.
     *
     * @return the sanitized name, or {@code null} if the name must be rejected.
     */
    @Nullable
    static String sanitizeFileName(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String cleaned = name.replace('\\', '/');
        final int lastSeparator = cleaned.lastIndexOf('/');
        if (lastSeparator >= 0) {
            cleaned = cleaned.substring(lastSeparator + 1);
        }
        cleaned = cleaned.trim();
        if (cleaned.isEmpty() || cleaned.length() > 255) {
            return null;
        }
        for (int i = 0; i < cleaned.length(); i++) {
            if (Character.isISOControl(cleaned.charAt(i))) {
                return null;
            }
        }
        return cleaned;
    }
}