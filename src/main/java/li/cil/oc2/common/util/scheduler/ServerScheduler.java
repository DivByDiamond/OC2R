package li.cil.oc2.common.util.scheduler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import li.cil.oc2.common.util.event.ListenerCollection;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class ServerScheduler {
    private static final TickScheduler globalTickScheduler = new TickScheduler();
    private static final Map<LevelAccessor, TickScheduler> levelTickSchedulers =
            Collections.synchronizedMap(Collections.synchronizedMap(Collections.synchronizedMap(new WeakHashMap<>())));
    private static final Map<LevelAccessor, SimpleScheduler> levelUnloadSchedulers =
            Collections.synchronizedMap(Collections.synchronizedMap(Collections.synchronizedMap(new WeakHashMap<>())));
    private static final Map<LevelAccessor, Map<ChunkPos, ListenerCollection>>
            chunkLoadSchedulers = Collections.synchronizedMap(Collections.synchronizedMap(Collections.synchronizedMap(new WeakHashMap<>())));
    private static final Map<LevelAccessor, Map<ChunkPos, ListenerCollection>>
            chunkUnloadSchedulers = Collections.synchronizedMap(Collections.synchronizedMap(Collections.synchronizedMap(new WeakHashMap<>())));

    public static void initialize() {
        CommonEvents.SERVER_STOPPED.register(server -> EventHandler.handleServerStopped());
        CommonEvents.LEVEL_UNLOAD.register(EventHandler::handleLevelUnload);
        CommonEvents.CHUNK_LOAD.register(EventHandler::handleChunkLoad);
        CommonEvents.CHUNK_UNLOAD.register(EventHandler::handleChunkUnload);
        CommonEvents.SERVER_TICK_START.register(server -> EventHandler.handleServerTick());
        CommonEvents.LEVEL_TICK_START.register(EventHandler::handleLevelTick);
    }

    public static void schedule(final Runnable runnable) {
        schedule(runnable, 0);
    }

    public static void schedule(final Runnable runnable, final int afterTicks) {
        globalTickScheduler.schedule(runnable, afterTicks);
    }

    public static void schedule(final LevelAccessor level, final Runnable runnable) {
        schedule(level, runnable, 0);
    }

    public static void schedule(
            final LevelAccessor level, final Runnable runnable, final int afterTicks) {
        final TickScheduler scheduler =
                levelTickSchedulers.computeIfAbsent(level, w -> new TickScheduler());
        scheduler.schedule(runnable, afterTicks);
    }

    public static void scheduleOnUnload(final LevelAccessor level, final Runnable listener) {
        levelUnloadSchedulers.computeIfAbsent(level, unused -> new SimpleScheduler()).add(listener);
    }

    public static void cancelOnUnload(
            @Nullable final LevelAccessor level, final Runnable listener) {
        if (level == null) {
            return;
        }

        final SimpleScheduler scheduler = levelUnloadSchedulers.get(level);
        if (scheduler != null) {
            scheduler.remove(listener);
        }
    }

    public static void subscribeOnLoad(
            final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        chunkLoadSchedulers
                .computeIfAbsent(level, unused -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkPos, unused -> new ListenerCollection())
                .add(listener);
    }

    public static void unsubscribeOnLoad(
            @Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        if (level == null) {
            return;
        }

        final Map<ChunkPos, ListenerCollection> chunkMap = chunkLoadSchedulers.get(level);
        if (chunkMap == null) {
            return;
        }

        final ListenerCollection listeners = chunkMap.get(chunkPos);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                chunkMap.remove(chunkPos);
            }
        }
    }

    public static void subscribeOnUnload(
            final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        chunkUnloadSchedulers
                .computeIfAbsent(level, unused -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkPos, unused -> new ListenerCollection())
                .add(listener);
    }

    public static void unsubscribeOnUnload(
            @Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        if (level == null) {
            return;
        }

        final Map<ChunkPos, ListenerCollection> chunkMap = chunkUnloadSchedulers.get(level);
        if (chunkMap == null) {
            return;
        }

        final ListenerCollection listeners = chunkMap.get(chunkPos);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                chunkMap.remove(chunkPos);
            }
        }
    }

    private static final class EventHandler {
        private static void handleServerStopped() {
            globalTickScheduler.clear();
            levelTickSchedulers.clear();
            levelUnloadSchedulers.clear();
            chunkLoadSchedulers.clear();
            chunkUnloadSchedulers.clear();
        }

        private static void handleLevelUnload(final LevelAccessor level) {

            levelTickSchedulers.remove(level);
            chunkLoadSchedulers.remove(level);
            chunkUnloadSchedulers.remove(level);

            final SimpleScheduler scheduler = levelUnloadSchedulers.remove(level);
            if (scheduler != null) {
                scheduler.run();
            }
        }

        private static void handleChunkLoad(final LevelAccessor level, final ChunkAccess chunk) {
            final Map<ChunkPos, ListenerCollection> chunkMap =
                    chunkLoadSchedulers.get(level);
            if (chunkMap == null) {
                return;
            }

            final ListenerCollection listeners = chunkMap.get(chunk.getPos());
            if (listeners != null) {
                listeners.run();
            }
        }

        private static void handleChunkUnload(final LevelAccessor level, final ChunkAccess chunk) {
            final Map<ChunkPos, ListenerCollection> chunkMap =
                    chunkUnloadSchedulers.get(level);
            if (chunkMap == null) {
                return;
            }

            final ListenerCollection listeners = chunkMap.get(chunk.getPos());
            if (listeners != null) {
                listeners.run();
            }
        }

        private static void handleServerTick() {
            globalTickScheduler.tick();

            // values() of a synchronizedMap must be iterated under the map's lock; snapshot it so
            // scheduler.tick() (arbitrary callbacks) never runs while the lock is held.
            final List<TickScheduler> schedulers;
            synchronized (levelTickSchedulers) { // NOPMD - must hold the synchronizedMap monitor to iterate values()
                schedulers = new ArrayList<>(levelTickSchedulers.values());
            }
            for (final TickScheduler scheduler : schedulers) {
                scheduler.tick();
            }
        }

        private static void handleLevelTick(final Level level) {
            globalTickScheduler.processQueue();

            final TickScheduler scheduler = levelTickSchedulers.get(level);
            if (scheduler != null) {
                scheduler.processQueue();
            }
        }
    }
}