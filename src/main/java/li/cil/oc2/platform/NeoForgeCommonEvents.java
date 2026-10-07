package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.platform.event.CommonEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Fires the loader-independent {@link CommonEvents} from NeoForge's game event bus. */
@EventBusSubscriber(modid = API.MOD_ID)
public final class NeoForgeCommonEvents {
    private NeoForgeCommonEvents() {}

    @SubscribeEvent
    public static void onServerAboutToStart(final ServerAboutToStartEvent event) {
        CommonEvents.SERVER_ABOUT_TO_START.fire(listener -> listener.accept(event.getServer()));
    }

    @SubscribeEvent
    public static void onServerStarted(final ServerStartedEvent event) {
        CommonEvents.SERVER_STARTED.fire(listener -> listener.accept(event.getServer()));
    }

    @SubscribeEvent
    public static void onServerStopping(final ServerStoppingEvent event) {
        CommonEvents.SERVER_STOPPING.fire(listener -> listener.accept(event.getServer()));
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        CommonEvents.SERVER_STOPPED.fire(listener -> listener.accept(event.getServer()));
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Pre event) {
        CommonEvents.SERVER_TICK_START.fire(listener -> listener.accept(event.getServer()));
    }

    @SubscribeEvent
    public static void onLevelTick(final LevelTickEvent.Pre event) {
        CommonEvents.LEVEL_TICK_START.fire(listener -> listener.accept(event.getLevel()));
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        CommonEvents.LEVEL_UNLOAD.fire(listener -> listener.accept(event.getLevel()));
    }

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event) {
        CommonEvents.CHUNK_LOAD.fire(
                listener -> listener.onChunk(event.getLevel(), event.getChunk()));
    }

    @SubscribeEvent
    public static void onChunkUnload(final ChunkEvent.Unload event) {
        CommonEvents.CHUNK_UNLOAD.fire(
                listener -> listener.onChunk(event.getLevel(), event.getChunk()));
    }

    @SubscribeEvent
    public static void onAddReloadListener(final AddReloadListenerEvent event) {
        CommonEvents.ADD_RELOAD_LISTENER.fire(listener -> listener.accept(event::addListener));
    }
}
