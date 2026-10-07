package li.cil.oc2.fabric;

import li.cil.oc2.client.ClientEventListeners;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.common.hooks.ClientProxy;
import li.cil.oc2.platform.FabricClientEvents;
import li.cil.oc2.platform.FabricClientProxy;
import li.cil.oc2.platform.FabricClientRegistrar;
import li.cil.oc2.platform.FabricClientSetup;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Fabric client entrypoint. Runs after {@link OC2RFabric}, so registry entries are bound; the client
 * message receivers are registered by the message registrar during the common initialisation.
 */
@Environment(EnvType.CLIENT)
public final class OC2RFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientEvents.register();
        FabricClientBlockEntityHooks.register();

        ClientProxy.set(new FabricClientProxy());
        ClientEventListeners.register();
        ClientSetup.register(new FabricClientRegistrar());
        FabricClientSetup.register();
    }
}
