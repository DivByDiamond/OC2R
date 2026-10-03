package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.platform.event.ClientEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Fires the loader-independent {@link ClientEvents} from NeoForge's game event bus. */
@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClientEvents {
    private NeoForgeClientEvents() {}

    @SubscribeEvent
    public static void onClientTickStart(final ClientTickEvent.Pre event) {
        ClientEvents.CLIENT_TICK_START.fire(Runnable::run);
    }
}
