package li.cil.oc2.common.setup;

import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import li.cil.oc2.common.inet.internet.InternetManagerImpl;
import li.cil.oc2.common.integration.Integrations;
import li.cil.oc2.common.util.scheduler.ServerScheduler;
import li.cil.oc2.common.vxlan.TunnelManager;

public final class CommonSetup {
    /** Runs once all mods are constructed; the loaders call this from their setup event or initializer. */
    public static void run() {
        Integrations.initialize();
        InternetManagerImpl.initialize();
        RPCMethodParameterTypeAdapters.initialize();
        ServerScheduler.initialize();
        TunnelManager.initialize();
    }

    private CommonSetup() {
    }
}
