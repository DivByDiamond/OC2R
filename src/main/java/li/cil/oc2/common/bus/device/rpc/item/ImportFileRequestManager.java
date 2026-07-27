package li.cil.oc2.common.bus.device.rpc.item;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ServerCanceledImportFileMessage;

import net.minecraft.server.level.ServerPlayer;

final class ImportFileRequestManager {
    private static final Int2ObjectArrayMap<ImportFileRequest> importingDevices =
            new Int2ObjectArrayMap<>();
    private static int nextImportId = 1;

    static int registerRequest(final FileImportExportCardItemDevice device) {
        final int id = nextImportId++;
        synchronized (importingDevices) {
            importingDevices.put(id, new ImportFileRequest(device));
        }
        return id;
    }

    static void removeRequest(final int id) {
        synchronized (importingDevices) {
            importingDevices.remove(id);
        }
    }

    public static void setImportedFile(final int id, final String name, final byte[] data) {
        synchronized (importingDevices) {
            final ImportFileRequest request = importingDevices.remove(id);
            if (request != null) {
                final FileImportExportCardItemDevice device = request.Device.get();
                if (device != null) {
                    device.importedFile = new ImportedFile(name, data);
                    final ServerCanceledImportFileMessage message =
                            new ServerCanceledImportFileMessage(id);
                    for (final ServerPlayer serverPlayer : request.PendingPlayers) {
                        Network.sendToClient(message, serverPlayer);
                    }
                }
            }
        }
    }

    public static void cancelImport(final ServerPlayer player, final int id) {
        synchronized (importingDevices) {
            final ImportFileRequest request = importingDevices.get(id);
            if (request != null) {
                request.PendingPlayers.remove(player);
                if (request.PendingPlayers.isEmpty()) {
                    importingDevices.remove(id);
                    final FileImportExportCardItemDevice device = request.Device.get();
                    if (device != null) {
                        device.state = ImportExportState.IMPORT_CANCELED;
                    }
                }
            }
        }
    }
}
