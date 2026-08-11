package li.cil.oc2.common.bus.device.rpc.item.file.request;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import li.cil.oc2.common.bus.device.rpc.item.card.FileImportExportCardItemDevice;
import net.minecraft.server.level.ServerPlayer;

final class ImportFileRequest {
    public final Set<ServerPlayer> PendingPlayers = Collections.newSetFromMap(new WeakHashMap<>());
    public final WeakReference<FileImportExportCardItemDevice> Device;

    ImportFileRequest(final FileImportExportCardItemDevice device) {
        Device = new WeakReference<>(device);
    }
}