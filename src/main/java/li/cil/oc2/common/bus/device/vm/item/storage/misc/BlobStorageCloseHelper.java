package li.cil.oc2.common.bus.device.vm.item.storage.misc;

import java.util.UUID;
import java.util.concurrent.CompletionException;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.serialization.BlobStorage;
import org.apache.logging.log4j.Logger;

public final class BlobStorageCloseHelper {
    public static void closeBlob(final Logger logger, final UUID blobHandle) {
        if (blobHandle == null) return;
        if (AsyncConfig.SERVER.asyncStorageOperations.get()) {
            BlobStorage.closeAsync(blobHandle)
                    .exceptionally(
                            e -> {
                                logger.error("Error closing blob: " + blobHandle, e);
                                return null;
                            });
        } else {
            try {
                BlobStorage.closeAsync(blobHandle).join();
            } catch (final CompletionException e) {
                logger.error("Error closing blob: " + blobHandle, e);
            }
        }
    }
}
