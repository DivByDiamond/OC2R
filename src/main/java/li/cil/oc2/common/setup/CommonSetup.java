package li.cil.oc2.common.setup;

import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.inet.internet.InternetManagerImpl;
import li.cil.oc2.common.integration.Integrations;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.common.vxlan.TunnelManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class CommonSetup {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLCommonSetupEvent event) {
        Integrations.initialize();
        InternetManagerImpl.initialize();
        RPCMethodParameterTypeAdapters.initialize();
        ServerScheduler.initialize();
        TunnelManager.initialize();
    }
}