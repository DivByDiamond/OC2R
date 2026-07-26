package li.cil.oc2.common.serialization;

import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.util.AsyncUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

final class BlobChannelManager {
    private static final Logger LOGGER = LogManager.getLogger();


    private static final Map<UUID, FileChannel> BLOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, CompletableFuture<FileChannel>> PENDING_OPERATIONS = new ConcurrentHashMap<>();


    private static Path getBlobPath(final UUID handle) {
        if (BlobStorage.dataDirectory == null) {
            throw new IllegalStateException("Data directory not initialized. Server not set?");
        }
        return BlobStorage.dataDirectory.resolve(handle.toString());
    }


    static CompletableFuture<FileChannel> openAsync(final UUID handle) {
        final FileChannel existingChannel = BLOBS.get(handle);
        if (existingChannel != null && existingChannel.isOpen()) {
            return CompletableFuture.completedFuture(existingChannel);
        }

        return PENDING_OPERATIONS.computeIfAbsent(handle, h ->
            AsyncUtils.runAsync(() -> {
                try {
                    final Path path = getBlobPath(h);
                    final FileChannel channel = new RandomAccessFile(path.toFile(), "rw").getChannel();
                    BLOBS.put(h, channel);
                    return channel;
                } catch (final IOException e) {
                    LOGGER.error("Failed to open blob: " + h, e);
                    throw new CompletionException("Failed to open blob: " + h, e);
                } finally {
                    PENDING_OPERATIONS.remove(h);
                }
            }, "Open blob " + h));
    }


    static CompletableFuture<Void> closeAsync(final UUID handle) {
        boolean debug = false;
        try {
            debug = AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get();
        } catch (IllegalStateException ignored) {
        }

        final boolean finalDebug = debug;
        return AsyncUtils.runAsync(() -> {
            try {
                final FileChannel blob = BLOBS.remove(handle);
                if (blob != null) {
                    blob.close();
                    if (finalDebug) {
                        LOGGER.debug("Closed blob: {}", handle);
                    }
                }
            } catch (final IOException e) {
                LOGGER.error("Error closing blob: " + handle, e);
                throw new CompletionException(e);
            }
        }, "Close blob " + handle);
    }


    static CompletableFuture<Void> deleteAsync(final UUID handle) {
        boolean debug = false;
        try {
            debug = AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get();
        } catch (IllegalStateException ignored) {
        }

        final boolean finalDebug = debug;
        return AsyncUtils.runAsync(() -> {
            final Path path = getBlobPath(handle);
            try {
                final boolean deleted = Files.deleteIfExists(path);
                if (deleted && finalDebug) {
                    LOGGER.debug("Deleted blob file: {}", path);
                }
            } catch (final IOException e) {
                LOGGER.error("Error deleting blob file: " + path, e);
                throw new CompletionException(e);
            }
            return null;
        }, "Deleting blob " + handle);
    }


    static void closeAll() {
        for (final CompletableFuture<FileChannel> future : PENDING_OPERATIONS.values()) {
            future.cancel(true);
        }
        PENDING_OPERATIONS.clear();

        for (final FileChannel blob : BLOBS.values()) {
            try {
                blob.close();
            } catch (final IOException e) {
                LOGGER.error("Error closing blob channel", e);
            }
        }
        BLOBS.clear();
    }
}
