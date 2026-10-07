package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.common.capabilities.CapabilityProviders;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Hands NeoForge's capability registration event to the loader-independent providers. */
@EventBusSubscriber(modid = API.MOD_ID)
public final class NeoForgeCapabilitySetup {
    private NeoForgeCapabilitySetup() {}

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        CapabilityProviders.registerAll(new NeoForgeCapabilityRegistrar(event));
    }
}
