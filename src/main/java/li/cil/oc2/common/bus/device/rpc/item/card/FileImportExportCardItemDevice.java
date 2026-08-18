package li.cil.oc2.common.bus.device.rpc.item.card;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.rpc.item.AbstractItemRPCDevice;
import li.cil.oc2.common.bus.device.rpc.item.file.ExportedFile;
import li.cil.oc2.common.bus.device.rpc.item.file.ImportExportState;
import li.cil.oc2.common.bus.device.rpc.item.file.ImportedFile;
import li.cil.oc2.common.bus.device.rpc.item.file.ImportedFileInfo;
import li.cil.oc2.common.bus.device.rpc.item.file.request.ImportFileRequestManager;
import li.cil.oc2.common.network.NetworkMessages;
import li.cil.oc2.common.network.message.file.ExportedFileMessage;
import li.cil.oc2.common.network.message.file.RequestImportedFileMessage;
import li.cil.oc2.common.network.message.misc.MultipartMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class FileImportExportCardItemDevice extends AbstractItemRPCDevice
        implements DocumentedDevice {
    public static final int MAX_TRANSFERRED_FILE_SIZE = Constants.MEGABYTE - 1;

    private static final String BEGIN_EXPORT_FILE = "beginExportFile";
    private static final String WRITE_EXPORT_FILE = "writeExportFile";
    private static final String FINISH_EXPORT_FILE = "finishExportFile";
    private static final String REQUEST_IMPORT_FILE = "requestImportFile";
    private static final String BEGIN_IMPORT_FILE = "beginImportFile";
    private static final String READ_IMPORT_FILE = "readImportFile";
    private static final String RESET_CMD = "reset";
    private static final String NAME = "name";
    private static final String DATA = "data";

    private final TerminalUserProvider userProvider;
    public ImportExportState state;
    private ExportedFile exportedFile;
    private int importingId;
    public ImportedFile importedFile;

    public FileImportExportCardItemDevice(
            final ItemStack identity, final TerminalUserProvider userProvider) {
        super(identity, "file_import_export");
        this.userProvider = userProvider;
    }

    public static void setImportedFile(final int id, final String name, final byte[] data) {
        ImportFileRequestManager.setImportedFile(id, name, data);
    }

    public static void cancelImport(final ServerPlayer player, final int id) {
        ImportFileRequestManager.cancelImport(player, id);
    }

    @Override
    public void unmount() {
        reset();
    }

    @Callback(name = BEGIN_EXPORT_FILE, synchronize = false)
    public void beginExportFile(@Parameter(NAME) final String name) {
        if (state != ImportExportState.IDLE) throw new IllegalStateException("invalid state");
        if (StringUtil.isNullOrEmpty(name))
            throw new IllegalArgumentException("name must not be empty");
        state = ImportExportState.EXPORTING;
        exportedFile = new ExportedFile(name);
    }

    @Callback(name = WRITE_EXPORT_FILE, synchronize = false)
    public void writeExportFile(@Parameter(DATA) @Nullable final byte[] data) throws IOException {
        if (state != ImportExportState.EXPORTING) throw new IllegalStateException("invalid state");
        if (data == null) throw new IllegalArgumentException("data is required");
        exportedFile.data.write(data);
        if (exportedFile.data.size() > MAX_TRANSFERRED_FILE_SIZE) {
            reset();
            throw new IllegalArgumentException("exported file too large");
        }
    }

    @Callback(name = FINISH_EXPORT_FILE)
    public void finishExportFile() {
        if (state != ImportExportState.EXPORTING) throw new IllegalStateException("invalid state");
        try {
            final ExportedFileMessage message =
                    new ExportedFileMessage(exportedFile.name, exportedFile.data.toByteArray());
            for (final Player player : userProvider.getTerminalUsers()) {
                if (player instanceof final ServerPlayer serverPlayer) {
                    MultipartMessage.sendToClient(message, serverPlayer);
                }
            }
        } finally {
            reset();
        }
    }

    @Callback(name = REQUEST_IMPORT_FILE)
    public boolean requestImportFile() {
        if (state != ImportExportState.IDLE) throw new IllegalStateException("invalid state");
        final List<ServerPlayer> players = new ArrayList<>();
        for (final Player player : userProvider.getTerminalUsers()) {
            if (player instanceof final ServerPlayer serverPlayer) players.add(serverPlayer);
        }
        if (players.isEmpty()) return false;
        state = ImportExportState.IMPORT_REQUESTED;
        importingId = ImportFileRequestManager.registerRequest(this);
        final RequestImportedFileMessage message = new RequestImportedFileMessage(importingId);
        for (final ServerPlayer serverPlayer : players) {
            NetworkMessages.sendToClient(message, serverPlayer);
        }
        return true;
    }

    @Nullable
    @Callback(name = BEGIN_IMPORT_FILE)
    public ImportedFileInfo beginImportFile() {
        if (state == ImportExportState.IMPORT_CANCELED) {
            reset();
            throw new IllegalStateException("import was canceled");
        }
        if (state != ImportExportState.IMPORT_REQUESTED)
            throw new IllegalStateException("invalid state");
        if (importedFile == null) return null;
        state = ImportExportState.IMPORTING;
        return new ImportedFileInfo(importedFile.name, importedFile.size);
    }

    @Nullable
    @Callback(name = READ_IMPORT_FILE)
    public byte[] readImportFile() throws IOException {
        if (state == ImportExportState.IMPORT_CANCELED) {
            reset();
            throw new IllegalStateException("import was canceled");
        }
        if (state != ImportExportState.IMPORTING) throw new IllegalStateException("invalid state");
        if (importedFile == null) return new byte[0];
        final byte[] buffer = new byte[512];
        final int count = importedFile.data.read(buffer);
        if (count <= 0) {
            reset();
            return new byte[0];
        }
        if (count < buffer.length) {
            final byte[] data = new byte[count];
            System.arraycopy(buffer, 0, data, 0, count);
            return data;
        } else {
            return buffer;
        }
    }

    @Callback(name = RESET_CMD)
    public void reset() {
        state = ImportExportState.IDLE;
        exportedFile = null;
        importedFile = null;
        ImportFileRequestManager.removeRequest(importingId);
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        FileImportExportCardItemDeviceDoc.visit(visitor);
    }
}