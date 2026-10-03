package li.cil.oc2.common.config;

import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Loader-independent reaction to a (re)loaded config file; the loaders call it from their config events. */
public final class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void handleConfigLoaded(final boolean isClientConfig) {
        if (isClientConfig) {
            ClientSpec.loadValues();
        } else {
            CommonSpec.loadValues();
            LOGGER.debug("captureInputMode={}", Config.captureInputMode);
        }
    }

    private ConfigManager() {
    }
}
