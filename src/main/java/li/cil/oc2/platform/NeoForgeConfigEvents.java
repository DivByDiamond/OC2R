package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.common.config.ConfigManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/** Forwards NeoForge config load and reload events to the shared {@link ConfigManager}. */
@SuppressWarnings("unused")
@EventBusSubscriber(modid = API.MOD_ID)
public final class NeoForgeConfigEvents {
    @SubscribeEvent
    public static void handleModConfigEvent(final ModConfigEvent event) {
        ConfigManager.handleConfigLoaded(event.getConfig().getType() == ModConfig.Type.CLIENT);
    }

    private NeoForgeConfigEvents() {
    }
}
