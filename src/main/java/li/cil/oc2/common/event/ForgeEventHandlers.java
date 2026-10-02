package li.cil.oc2.common.event;

import javax.annotation.Nullable;
import li.cil.oc2.common.util.async.AsyncExecutorHelper;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles Forge lifecycle events to ensure proper initialization and cleanup of async operations.
 */
public final class ForgeEventHandlers {
    private static final Logger LOGGER = LogManager.getLogger();
    private static MinecraftServer server;

    /**
     * Get the current Minecraft server instance.
     *
     * @return The current Minecraft server instance, or null if not available.
     */
    @Nullable
    public static MinecraftServer getCurrentServer() {
        return server;
    }

    /** Subscribes to the server lifecycle events this class tracks. */
    public static void register() {
        CommonEvents.SERVER_ABOUT_TO_START.register(ForgeEventHandlers::handleServerAboutToStart);
        CommonEvents.SERVER_STOPPED.register(stoppedServer -> handleServerStopped());
    }

    private static void handleServerAboutToStart(final MinecraftServer startingServer) {
        server = startingServer;
        LOGGER.info("Server starting, initializing async components");
    }

    private static void handleServerStopped() {
        LOGGER.info("Server stopped, cleaning up async components");
        try {
            AsyncExecutorHelper.shutdown();
        } catch (final Exception e) {
            LOGGER.error("Error during async component shutdown", e);
        } finally {
            server = null;
        }
    }
}