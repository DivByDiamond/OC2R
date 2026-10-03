package li.cil.oc2.platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import li.cil.oc2.platform.event.CommonEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/** Fires the loader-independent {@link CommonEvents} from Fabric API callbacks (server side). */
public final class FabricCommonEvents {
    private static final String MOD_ID = "oc2r";
    private static int reloadListenerCount;

    private FabricCommonEvents() {}

    public static void register() {
        // Fabric has no "about to start" hook ahead of world loading; SERVER_STARTING is the
        // closest and runs before any level is loaded.
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                CommonEvents.SERVER_ABOUT_TO_START.fire(listener -> listener.accept(server)));
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                CommonEvents.SERVER_STARTED.fire(listener -> listener.accept(server)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                CommonEvents.SERVER_STOPPING.fire(listener -> listener.accept(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server ->
                CommonEvents.SERVER_STOPPED.fire(listener -> listener.accept(server)));
        ServerTickEvents.START_SERVER_TICK.register(server ->
                CommonEvents.SERVER_TICK_START.fire(listener -> listener.accept(server)));
        ServerTickEvents.START_WORLD_TICK.register(level ->
                CommonEvents.LEVEL_TICK_START.fire(listener -> listener.accept(level)));
        ServerWorldEvents.UNLOAD.register((server, level) ->
                CommonEvents.LEVEL_UNLOAD.fire(listener -> listener.accept(level)));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                CommonEvents.CHUNK_LOAD.fire(listener -> listener.onChunk(level, chunk)));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
                CommonEvents.CHUNK_UNLOAD.fire(listener -> listener.onChunk(level, chunk)));

        // Fabric registers reload listeners once, by id, rather than per reload like NeoForge's
        // AddReloadListenerEvent; listeners handed to the event are therefore registered right away.
        CommonEvents.ADD_RELOAD_LISTENER.fire(listener -> listener.accept(FabricCommonEvents::addReloadListener));
    }

    private static void addReloadListener(final PreparableReloadListener listener) {
        final ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "reload_listener_" + reloadListenerCount++);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Identified(id, listener));
    }

    private record Identified(ResourceLocation id, PreparableReloadListener delegate)
            implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return id;
        }

        @Override
        public CompletableFuture<Void> reload(
                final PreparationBarrier barrier,
                final ResourceManager manager,
                final ProfilerFiller preparationsProfiler,
                final ProfilerFiller reloadProfiler,
                final Executor backgroundExecutor,
                final Executor gameExecutor) {
            return delegate.reload(
                    barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        }
    }
}
