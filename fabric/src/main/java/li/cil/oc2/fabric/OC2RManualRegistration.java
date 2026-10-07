package li.cil.oc2.fabric;

import li.cil.manual.api.platform.FabricManualInitializer;
import li.cil.oc2.client.manual.Manuals;

/**
 * Entrypoint of the Markdown Manual library ({@code markdown_manual:registration}). The library
 * creates its registries in its own client initializer, so the manual, tabs and providers can only
 * be registered from here (the same registrations NeoForge does from the mod constructor).
 */
public final class OC2RManualRegistration implements FabricManualInitializer {
    @Override
    public void registerManualObjects() {
        Manuals.initialize();
    }
}
