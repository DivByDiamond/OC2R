package li.cil.oc2.common.bus.device.rpc.item.file.request;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.concurrent.locks.ReentrantLock;
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
        final int id = nextImportId++;
        lock.lock();
        try {

            importingDevices.put(id, new ImportFileRequest(device));
        
        } finally {
            lock.unlock();
        }
        return id;
    }

    public static void removeRequest(final int id) {
        lock.lock();
        try {

            importingDevices.remove(id);
        
        } finally {
            lock.unlock();
        }
    }

    public static void setImportedFile(final int id, final String name, final byte[] data) {
        lock.lock();
        try {

            final ImportFileRequest request = importingDevices.remove(id);
            if (request != null) {
                final FileImportExportCardItemDevice device = request.Device.get();
                if (device != null) {
                    device.importedFile = new ImportedFile(name, data);
                    final ServerCanceledImportFileMessage message =
                            new ServerCanceledImportFileMessage(id);
                    for (final ServerPlayer serverPlayer : request.PendingPlayers) {
                        NetworkMessages.sendToClient(message, serverPlayer);
                    }
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
}