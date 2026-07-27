package li.cil.oc2.common.util.async;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import li.cil.oc2.common.config.AsyncConfig;
import li.cil.oc2.common.event.ForgeEventHandlers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AsyncExecutorHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile ExecutorService asyncExecutor;

    static {
        asyncExecutor = createExecutor();
    }

    static ExecutorService getAsyncExecutor() {
        return ensureExecutor();
    }

    static boolean isShutdown() {
        return asyncExecutor == null || asyncExecutor.isShutdown();
    }

    public static void shutdown() {
        final ExecutorService executor = asyncExecutor;
        if (executor == null || executor.isShutdown()) {
            return;
        }

        boolean debug = false;
        try {
            debug = AsyncConfig.SERVER != null && AsyncConfig.SERVER.enableSuperDebug.get();
        } catch (IllegalStateException ignored) {
        }

        if (debug) {
            LOGGER.info("Initiating async executor shutdown...");
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                if (debug) {
                    LOGGER.warn(
                            "Async executor did not shut down within timeout, forcing immediate"
                                    + " shutdown");
                } else {
                    LOGGER.warn(
                            "Async executor did not shut down within timeout, forcing immediate"
                                    + " shutdown");
                }

                final var runningTasks = executor.shutdownNow();

                if (debug && !runningTasks.isEmpty()) {
                    LOGGER.warn("Cancelled {} running tasks", runningTasks.size());
                }

                if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    LOGGER.warn("Some tasks did not respond to cancellation");
                }
            }
        } catch (final InterruptedException e) {
            LOGGER.warn(
                    "Interrupted while waiting for async executor to shut down, forcing immediate"
                            + " shutdown",
                    e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static ExecutorService createExecutor() {
        return new ForkJoinPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                (t, e) -> LOGGER.error("Uncaught exception in async executor thread", e),
                true) {
            @Override
            public List<Runnable> shutdownNow() {
                List<Runnable> tasks = super.shutdownNow();
                for (Thread worker : getActiveWorkers()) {
                    worker.interrupt();
                }
                return tasks;
            }

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