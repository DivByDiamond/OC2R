package li.cil.oc2.common.serialization;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.common.config.AsyncConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BlobStorage {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final LevelResource BLOBS_FOLDER_NAME = new LevelResource(API.MOD_ID + "-blobs");
    private static final Set<UUID> ACTIVE_HANDLES = ConcurrentHashMap.newKeySet();
    static volatile Path dataDirectory;

    static {
        Runtime.getRuntime()
                .addShutdownHook(new Thread(BlobStorage::close, "OC2 BlobStorage Shutdown"));
    }

    public static void setServer(final MinecraftServer server) {
        final Path newDataDir = server.getWorldPath(BLOBS_FOLDER_NAME);
        if (!newDataDir.equals(dataDirectory)) {
            close();
            dataDirectory = newDataDir;

            try {
                Files.createDirectories(dataDirectory);
                LOGGER.info("Blob storage directory initialized at: {}", dataDirectory);
            } catch (final IOException e) {
                LOGGER.error("Failed to create blob storage directory", e);
            }
        }
    }

    public static void close() {
        BlobChannelManager.closeAll();
        ACTIVE_HANDLES.clear();

        boolean debug = false;
        try {
            debug = AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get();
        } catch (IllegalStateException ignored) {
        }

        if (debug) {
            LOGGER.info("Closed all blob storage resources");
        }
    }

    public static UUID allocateHandle() {
        return UUID.randomUUID();
    }

    public static UUID validateHandle(@Nullable final UUID handle) {
        final UUID validated;
        if (handle == null
                || (handle.getMostSignificantBits() == 0
                        && handle.getLeastSignificantBits() == 0)) {
            validated = allocateHandle();
        } else {
            validated = handle;
        }
        ACTIVE_HANDLES.add(validated);
        return validated;
    }

    public static CompletableFuture<FileChannel> getOrOpenAsync(final UUID handle) {
        return BlobChannelManager.openAsync(handle);
    }

    @Deprecated(since = "1.21.1", forRemoval = true)
    public static synchronized FileChannel getOrOpen(final UUID handle) throws IOException {
        try {
            return getOrOpenAsync(handle).join();
        } catch (final CompletionException e) {
            if (e.getCause() instanceof IOException) {
                throw new IOException("Failed to open blob: " + handle, e);
            }
            throw new IOException("Failed to open blob: " + handle, e);
        }
    }

    public static CompletableFuture<Void> closeAsync(final UUID handle) {
        return BlobChannelManager.closeAsync(handle);
    }

    @Deprecated(since = "1.21.1", forRemoval = true)
    public static synchronized void close(final UUID handle) {
        try {
            closeAsync(handle).join();
        } catch (final CompletionException e) {
            LOGGER.error("Error in close operation for blob: " + handle, e);
        }
    }

    public static CompletableFuture<Void> deleteAsync(final UUID handle) {
        ACTIVE_HANDLES.remove(handle);
        return BlobChannelManager.deleteAsync(handle);
    }

    @Deprecated(since = "1.21.1", forRemoval = true)
    public static void delete(final UUID handle) {
        try {
            deleteAsync(handle).join();
        } catch (final CompletionException e) {
            LOGGER.error("Error in delete operation for blob: " + handle, e);
        }
    }

    public static void cleanupOrphaned() {
        final Path dir = dataDirectory;
        if (dir == null) {
            return;
        }

        final Set<UUID> active = Set.copyOf(ACTIVE_HANDLES);
        try (final Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                    .map(BlobStorage::parseHandle)
                    .flatMap(Optional::stream)
                    .filter(handle -> !active.contains(handle))
                    .forEach(
                            handle -> {
                                LOGGER.info("Deleting orphaned blob: {}", handle);
                                deleteAsync(handle);
                            });
        } catch (final IOException e) {
            LOGGER.error("Failed to list blob storage directory: {}", dir, e);
        }
    }

    private static Optional<UUID> parseHandle(final Path path) {
        try {
            return Optional.of(UUID.fromString(path.getFileName().toString()));
        } catch (final IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}