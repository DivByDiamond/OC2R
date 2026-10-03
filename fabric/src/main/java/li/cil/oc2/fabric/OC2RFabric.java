package li.cil.oc2.fabric;

import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint. Loader bridges are bound here as they are implemented. */
public final class OC2RFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Bridges (registry, network, energy, capabilities, events) are wired in by later steps.
    }
}
