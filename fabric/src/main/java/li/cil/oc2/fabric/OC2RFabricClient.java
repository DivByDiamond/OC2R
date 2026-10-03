package li.cil.oc2.fabric;

import li.cil.oc2.platform.FabricClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Fabric client entrypoint. */
@Environment(EnvType.CLIENT)
public final class OC2RFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientEvents.register();
        FabricClientBlockEntityHooks.register();
    }
}
