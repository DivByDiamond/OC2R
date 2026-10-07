package li.cil.oc2.common.serialization;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.platform.event.CommonEvents;

public final class BlobStorageEvents {
    private BlobStorageEvents() {}

    /** Subscribes to the server lifecycle events the blob storage follows. */
    public static void register() {
        CommonEvents.SERVER_ABOUT_TO_START.register(BlobStorage::setServer);
        CommonEvents.SERVER_STARTED.register(server -> handleServerStarted());
        CommonEvents.SERVER_STOPPED.register(server -> handleServerStopped());
    }

    private static void handleServerStarted() {
        ServerScheduler.schedule(
                BlobStorage::cleanupOrphaned, Constants.SECONDS_TO_TICKS * 5);
    }

    private static void handleServerStopped() {
        BlobStorage.close();
    }
}