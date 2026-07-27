package li.cil.oc2.common.util.async;

import java.util.concurrent.*;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.event.ForgeEventHandlers;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AsyncUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private AsyncUtils() {}

    public static <T> CompletableFuture<T> runAsync(Supplier<T> task, String description) {
        final ExecutorService executor = AsyncExecutorHelper.getAsyncExecutor();
        if (executor == null || executor.isShutdown()) {
            LOGGER.warn(
                    "Attempted to submit async task '{}' after executor was shut down",
                    description);
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new RejectedExecutionException("Executor has been shut down"));
            return failedFuture;
        }

        try {
            if (AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get()) {
                LOGGER.info("Starting async task: {}", description);
                logStackTrace("Async task stack trace");
            }
        } catch (IllegalStateException e) {
            LOGGER.trace("Config not loaded yet, skipping debug logging for: {}", description);
        }

        try {
            return CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return task.get();
                        } catch (final Exception t) {
                            LOGGER.error("Error in async task: " + description, t);
                            throw t;
                        } finally {
                            try {
                                if (AsyncConfig.SERVER != null
                                        && AsyncConfig.SERVER.enableSuperDebug.get()) {
                                    LOGGER.info("Completed async task: {}", description);
                                }
                            } catch (IllegalStateException e) {
                                LOGGER.trace(
                                        "Config not loaded yet, skipping debug logging for: {}",
                                        description);
                            }
                        }
                    },
                    executor);
        } catch (RejectedExecutionException e) {
            LOGGER.warn(
                    "Failed to submit async task '{}' - executor is shutting down", description, e);
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    public static CompletableFuture<Void> runAsync(Runnable task, String description) {
        return runAsync(
                () -> {
                    task.run();
                    return null;
                },
                description);
    }

    public static void logStackTrace(String message) {
        try {
            if (AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get()) {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                StringBuilder sb = new StringBuilder(message).append('\n');
                for (int i = 2; i < stackTrace.length; i++) {
                    sb.append("\tat ").append(stackTrace[i]).append('\n');
                }
                LOGGER.info(sb.toString());
            }
        } catch (IllegalStateException e) {
            LOGGER.trace("Config not loaded yet, skipping stack trace logging");
        }
    }

    public static <T> CompletableFuture<T> onServerThread(Supplier<T> task) {
        final MinecraftServer server = ForgeEventHandlers.getCurrentServer();
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No server available"));
        }

        final CompletableFuture<T> future = new CompletableFuture<>();

        server.execute(
                () -> {
                    if (AsyncConfig.SERVER.enableSuperDebug.get()) {
                        LOGGER.debug("Executing task on server thread");
                    }

                    try {
                        future.complete(task.get());
                    } catch (final Exception t) {
                        LOGGER.error("Error in server thread task", t);
                        future.completeExceptionally(t);
                    }
                });

        return future;
    }

    public static CompletableFuture<Void> onServerThread(Runnable task) {
        return onServerThread(
                () -> {
                    task.run();
                    return null;
                });
    }

    @Nullable
    public static Executor getServerExecutor() {
        final MinecraftServer server = ForgeEventHandlers.getCurrentServer();
        return server != null ? server : null;
    }
}