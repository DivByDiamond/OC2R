package li.cil.oc2.platform;

import li.cil.oc2.platform.event.ClientEvents;
import li.cil.oc2.platform.event.CommonEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

/**
 * Fires {@link ClientEvents}, and the client halves of {@link CommonEvents} (client level tick,
 * chunk and level unload), from Fabric API client callbacks. Only loaded on the physical client.
 */
@Environment(EnvType.CLIENT)
public final class FabricClientEvents {
    private FabricClientEvents() {}

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client ->
                ClientEvents.CLIENT_TICK_START.fire(Runnable::run));
        ClientTickEvents.START_WORLD_TICK.register(level ->
                CommonEvents.LEVEL_TICK_START.fire(listener -> listener.accept(level)));
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                CommonEvents.CHUNK_LOAD.fire(listener -> listener.onChunk(level, chunk)));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
                CommonEvents.CHUNK_UNLOAD.fire(listener -> listener.onChunk(level, chunk)));
        // Fabric has no per-level unload callback on the client; leaving the world is the point
        // where the client level goes away.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (Minecraft.getInstance().level != null) {
                CommonEvents.LEVEL_UNLOAD.fire(
                        listener -> listener.accept(Minecraft.getInstance().level));
            }
        });
    }
}
