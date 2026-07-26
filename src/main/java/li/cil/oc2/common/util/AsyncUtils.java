package li.cil.oc2.common.util;

import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.event.ForgeEventHandlers;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * Utility class for handling asynchronous operations with proper error handling and debugging.
 */
public final class AsyncUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    // Use a dedicated executor for async operations to avoid blocking the main server thread
    private static volatile ExecutorService asyncExecutor;

    static {
        asyncExecutor = createExecutor();
    }

    private static ExecutorService createExecutor() {
        return new ForkJoinPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            (t, e) -> LOGGER.error("Uncaught exception in async executor thread", e),
            true
        ) {
            @Override
            public List<Runnable> shutdownNow() {
                // Ensure all threads are interrupted when shutting down
                List<Runnable> tasks = super.shutdownNow();
                // Force an interrupt on all threads in the pool
                for (Thread worker : getActiveWorkers()) {
                    worker.interrupt();
                }
                return tasks;
            }

            // Helper method to get active worker threads
            private java.util.Set<Thread> getActiveWorkers() {
                java.util.Set<Thread> workers = java.util.concurrent.ConcurrentHashMap.newKeySet();
                for (Thread thread : Thread.getAllStackTraces().keySet()) {
                    if (thread.getName().startsWith("ForkJoinPool") && thread.isAlive()) {
                        workers.add(thread);
                    }
                }
                return workers;
            }
        };
    }

    /**
     * Gets the async executor service.
     *
     * @return The async executor service.
     */
    public static ExecutorService getAsyncExecutor() {
        return ensureExecutor();
    }

    // Prevent instantiation
    private AsyncUtils() {}

    /**
     * Runs a task asynchronously with proper error handling and debug logging.
     *
     * @param task the task to run
     * @param description a description of the task for logging purposes
     * @return a CompletableFuture that will complete when the task finishes
     */
    public static <T> CompletableFuture<T> runAsync(Supplier<T> task, String description) {
        final ExecutorService executor = ensureExecutor();
        if (executor == null || executor.isShutdown()) {
            LOGGER.warn("Attempted to submit async task '{}' after executor was shut down", description);
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RejectedExecutionException("Executor has been shut down"));
            return failedFuture;
        }

        try {
            if (AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get()) {
                LOGGER.info("Starting async task: {}", description);
                logStackTrace("Async task stack trace");
            }
        } catch (IllegalStateException e) {
            // Config not loaded yet, skip debug logging
            LOGGER.trace("Config not loaded yet, skipping debug logging for: {}", description);
        }

        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return task.get();
                } catch (Throwable t) {
                    LOGGER.error("Error in async task: " + description, t);
                    throw t;
                } finally {
                    try {
                        if (AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get()) {
                            LOGGER.info("Completed async task: {}", description);
                        }
                    } catch (IllegalStateException e) {
                        // Config not loaded yet, skip debug logging
                        LOGGER.trace("Config not loaded yet, skipping debug logging for: {}", description);
                    }
                }
            }, executor);
        } catch (RejectedExecutionException e) {
            LOGGER.warn("Failed to submit async task '{}' - executor is shutting down", description, e);
            CompletableFuture<T> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * Runs a task asynchronously with proper error handling and debug logging.
     *
     * @param task the task to run
     * @param description a description of the task for logging purposes
     * @return a CompletableFuture that will complete when the task finishes
     */
    public static CompletableFuture<Void> runAsync(Runnable task, String description) {
        return runAsync(() -> {
            task.run();
            return null;
        }, description);
    }

    /**
     * Checks if the async executor has been shut down.
     *
     * @return true if the executor has been shut down, false otherwise
     */
    public static boolean isShutdown() {
        return asyncExecutor == null || asyncExecutor.isShutdown();
    }

    /**
     * Logs the current stack trace if super debug mode is enabled.
     *
     * @param message the message to log with the stack trace
     */
    public static void logStackTrace(String message) {
        try {
            if (AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get()) {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                StringBuilder sb = new StringBuilder(message).append("\n");
                // Skip the first 2 elements (getStackTrace and this method)
                for (int i = 2; i < stackTrace.length; i++) {
                    sb.append("\tat ").append(stackTrace[i]).append("\n");
                }
                LOGGER.info(sb.toString());
            }
        } catch (IllegalStateException e) {
            // Config not loaded yet, skip debug logging
            LOGGER.trace("Config not loaded yet, skipping stack trace logging");
        }
    }

    /**
     * Schedules a task to run on the server thread.
     *
     * @param task The task to run on the server thread.
     * @param <T> The return type of the task.
     * @return A CompletableFuture that completes with the result of the task.
     */
    public static <T> CompletableFuture<T> onServerThread(Supplier<T> task) {
        final MinecraftServer server = ForgeEventHandlers.getCurrentServer();
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("No server available"));
        }

        final CompletableFuture<T> future = new CompletableFuture<>();

        server.execute(() -> {
            if (AsyncConfig.SERVER.enableSuperDebug.get()) {
                LOGGER.debug("Executing task on server thread");
            }

            try {
                future.complete(task.get());
            } catch (final Throwable t) {
                LOGGER.error("Error in server thread task", t);
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    /**
     * Schedules a task to run on the server thread.
     *
     * @param task The task to run on the server thread.
     * @return A CompletableFuture that completes when the task is done.
     */
    public static CompletableFuture<Void> onServerThread(Runnable task) {
        return onServerThread(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Gets the server's thread pool executor if available.
     *
     * @return The server's thread pool executor, or null if not available.
     */
    @Nullable
    public static Executor getServerExecutor() {
        final MinecraftServer server = ForgeEventHandlers.getCurrentServer();
        return server != null ? server : null;
    }

    /**
     * Shuts down the async executor. Should be called when the game is shutting down.
     * Uses a shorter timeout and more aggressive cancellation to speed up shutdown.
     */
    public static void shutdown() {
        final ExecutorService executor = asyncExecutor;
        if (executor == null || executor.isShutdown()) {
            return; // Already shut down
        }

        boolean debug = false;
        try {
            // Safely check debug config if available
            debug = AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get();
        } catch (IllegalStateException ignored) {
            // Config system might be shutting down, continue with debug disabled
        }

        if (debug) {
            LOGGER.info("Initiating async executor shutdown...");
        }

        // Disable new tasks from being submitted
        executor.shutdown();

        try {
            // Wait a short time for tasks to complete
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                if (debug) {
                    LOGGER.warn("Async executor did not shut down within timeout, forcing immediate shutdown");
                } else {
                    LOGGER.warn("Async executor did not shut down within timeout, forcing immediate shutdown");
                }

                // Cancel currently executing tasks
                final var runningTasks = executor.shutdownNow();

                if (debug && !runningTasks.isEmpty()) {
                    LOGGER.warn("Cancelled {} running tasks", runningTasks.size());
                }

                // Wait a bit more for tasks to respond to being cancelled
                if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    LOGGER.warn("Some tasks did not respond to cancellation");
                }
            }
        } catch (final InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for async executor to shut down, forcing immediate shutdown", e);
            // Try one last time with extreme prejudice
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static synchronized ExecutorService ensureExecutor() {
        if (asyncExecutor == null || asyncExecutor.isShutdown()) {
            if (ForgeEventHandlers.getCurrentServer() == null) {
                return asyncExecutor;
            }
            asyncExecutor = createExecutor();
            LOGGER.info("Async executor reinitialized");
        }
        return asyncExecutor;
    }
}
