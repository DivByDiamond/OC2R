/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import li.cil.oc2.api.API;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = API.MOD_ID)
public final class ConfigManager {
    @SubscribeEvent
    public static void handleModConfigEvent(final ModConfigEvent event) {
        final ModConfig.Type config = event.getConfig().getType();
        if (config == ModConfig.Type.CLIENT) {
            ClientSpec.loadValues();
        }
        else {
            CommonSpec.loadValues();
            System.out.println(Config.captureInputMode);
        }
    }
}
