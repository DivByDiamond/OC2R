package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.client.input.CaptureKeyBinding;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Registers the loader-specific host shortcut for world-visible guest input. */
@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeInputSetup {
    private NeoForgeInputSetup() {}

    /** Adds the remappable capture-release shortcut to Minecraft's Controls settings. */
    @SubscribeEvent
    public static void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        event.register(CaptureKeyBinding.RELEASE);
    }
}
