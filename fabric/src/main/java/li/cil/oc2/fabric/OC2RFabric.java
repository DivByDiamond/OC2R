package li.cil.oc2.fabric;

import li.cil.oc2.platform.FabricCommonEvents;
import li.cil.oc2.platform.FabricRegistryBridge;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint. Loader bridges are bound here as they are implemented. */
public final class OC2RFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricRegistryBridge.instance().bind();
        FabricCommonEvents.register();
        // Energy and capability bridges are wired in by later steps.
    }
}
