package li.cil.oc2.common.config;

import li.cil.oc2.api.API;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = API.MOD_ID)
public final class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void handleModConfigEvent(final ModConfigEvent event) {
        final ModConfig.Type config = event.getConfig().getType();
        if (config == ModConfig.Type.CLIENT) {
            ClientSpec.loadValues();
        } else {
            CommonSpec.loadValues();
            LOGGER.debug("captureInputMode={}", Config.captureInputMode);
        }
    }
}