package li.cil.oc2.common.util;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class ServerScheduler {
    private static final TickScheduler globalTickScheduler = new TickScheduler();
    private static final WeakHashMap<LevelAccessor, TickScheduler> levelTickSchedulers =
            new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, SimpleScheduler> levelUnloadSchedulers =
            new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, HashMap<ChunkPos, ListenerCollection>>
            chunkLoadSchedulers = new WeakHashMap<>();
    private static final WeakHashMap<LevelAccessor, HashMap<ChunkPos, ListenerCollection>>
            chunkUnloadSchedulers = new WeakHashMap<>();

    public static void initialize() {
        NeoForge.EVENT_BUS.register(EventHandler.class);
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
                .computeIfAbsent(level, unused -> new HashMap<>())
                .computeIfAbsent(chunkPos, unused -> new ListenerCollection())
                .add(listener);
    }

    public static void unsubscribeOnLoad(
            @Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        if (level == null) {
            return;
        }

        final HashMap<ChunkPos, ListenerCollection> chunkMap = chunkLoadSchedulers.get(level);
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
                .computeIfAbsent(level, unused -> new HashMap<>())
                .computeIfAbsent(chunkPos, unused -> new ListenerCollection())
                .add(listener);
    }

    public static void unsubscribeOnUnload(
            @Nullable final LevelAccessor level, final ChunkPos chunkPos, final Runnable listener) {
        if (level == null) {
            return;
        }

        final HashMap<ChunkPos, ListenerCollection> chunkMap = chunkUnloadSchedulers.get(level);
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
        @SubscribeEvent
        public static void handleServerStoppedEvent(final ServerStoppedEvent event) {
            globalTickScheduler.clear();
            levelTickSchedulers.clear();
            levelUnloadSchedulers.clear();
            chunkLoadSchedulers.clear();
            chunkUnloadSchedulers.clear();
        }

        @SubscribeEvent
        public static void handleLevelUnload(final LevelEvent.Unload event) {
            final LevelAccessor level = event.getLevel();

            levelTickSchedulers.remove(level);
            chunkLoadSchedulers.remove(level);
            chunkUnloadSchedulers.remove(level);

            final SimpleScheduler scheduler = levelUnloadSchedulers.remove(level);
            if (scheduler != null) {
                scheduler.run();
            }
        }

        @SubscribeEvent
        public static void handleChunkLoad(final ChunkEvent.Load event) {
            final HashMap<ChunkPos, ListenerCollection> chunkMap =
                    chunkLoadSchedulers.get(event.getLevel());
            if (chunkMap == null) {
                return;
            }

            final ListenerCollection listeners = chunkMap.get(event.getChunk().getPos());
            if (listeners != null) {
                listeners.run();
            }
        }

        @SubscribeEvent
        public static void handleChunkUnload(final ChunkEvent.Unload event) {
            final HashMap<ChunkPos, ListenerCollection> chunkMap =
                    chunkUnloadSchedulers.get(event.getLevel());
            if (chunkMap == null) {
                return;
            }

            final ListenerCollection listeners = chunkMap.get(event.getChunk().getPos());
            if (listeners != null) {
                listeners.run();
            }
        }

        @SubscribeEvent
        public static void handleServerTick(final ServerTickEvent.Pre event) {
            globalTickScheduler.tick();

            for (final TickScheduler scheduler : levelTickSchedulers.values()) {
                scheduler.tick();
            }
        }

        @SubscribeEvent
        public static void handleLevelTick(final LevelTickEvent.Pre event) {
            globalTickScheduler.processQueue();

            final TickScheduler scheduler = levelTickSchedulers.get(event.getLevel());
            if (scheduler != null) {
                scheduler.processQueue();
            }
        }
    }
}