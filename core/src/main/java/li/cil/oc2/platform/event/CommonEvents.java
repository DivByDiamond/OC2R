package li.cil.oc2.platform.event;

import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Game events common code subscribes to, independent of the loader. Mod code registers listeners
 * here (see {@code li.cil.oc2.common.event.CommonEventListeners}); each loader module fires them
 * from its own hooks, so no listener names a loader type.
 */
public final class CommonEvents {
    /** The server is about to start, before worlds load. */
    public static final Event<Consumer<MinecraftServer>> SERVER_ABOUT_TO_START = new Event<>();

    /** The server finished starting. */
    public static final Event<Consumer<MinecraftServer>> SERVER_STARTED = new Event<>();

    /** The server begins shutting down; worlds are still loaded. */
    public static final Event<Consumer<MinecraftServer>> SERVER_STOPPING = new Event<>();

    /** The server finished shutting down. */
    public static final Event<Consumer<MinecraftServer>> SERVER_STOPPED = new Event<>();

    /** Start of every server tick. */
    public static final Event<Consumer<MinecraftServer>> SERVER_TICK_START = new Event<>();

    /** Start of every tick of every level, client and server. */
    public static final Event<Consumer<net.minecraft.world.level.Level>> LEVEL_TICK_START =
            new Event<>();

    /** A level is being unloaded. */
    public static final Event<Consumer<LevelAccessor>> LEVEL_UNLOAD = new Event<>();

    /** A chunk finished loading. */
    public static final Event<ChunkListener> CHUNK_LOAD = new Event<>();

    /** A chunk is being unloaded. */
    public static final Event<ChunkListener> CHUNK_UNLOAD = new Event<>();

    /** Data pack reload listeners can be added; the consumer receives the listener to add. */
    public static final Event<Consumer<Consumer<PreparableReloadListener>>> ADD_RELOAD_LISTENER =
            new Event<>();

    private CommonEvents() {}

    /** A chunk lifecycle listener. */
    @FunctionalInterface
    public interface ChunkListener {
        void onChunk(LevelAccessor level, ChunkAccess chunk);
    }
}
