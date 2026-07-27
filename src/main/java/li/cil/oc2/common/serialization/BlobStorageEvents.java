package li.cil.oc2.common.serialization;

import li.cil.oc2.api.API;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = API.MOD_ID)
final class BlobStorageEvents {
    @SubscribeEvent
    public static void handleServerAboutToStart(final ServerAboutToStartEvent event) {
        BlobStorage.setServer(event.getServer());
    }

    @SubscribeEvent
    public static void handleServerStopped(final ServerStoppedEvent event) {
        BlobStorage.close();
    }
}