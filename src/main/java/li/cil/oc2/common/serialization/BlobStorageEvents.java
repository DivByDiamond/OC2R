package li.cil.oc2.common.serialization;

import li.cil.oc2.platform.event.CommonEvents;

public final class BlobStorageEvents {
    private BlobStorageEvents() {}

    /** Subscribes to the server lifecycle events the blob storage follows. */
    public static void register() {
        CommonEvents.SERVER_ABOUT_TO_START.register(BlobStorage::setServer);
        // Loaded devices are not the complete reference set: disks and flash chips may be
        // in unloaded chunks or offline player inventories. A startup sweep would erase them.
        // Only explicitly discarded, unpublished uploads can safely be deleted automatically.
        CommonEvents.SERVER_STOPPED.register(server -> handleServerStopped());
    }

    private static void handleServerStopped() {
        BlobStorage.close();
    }
}