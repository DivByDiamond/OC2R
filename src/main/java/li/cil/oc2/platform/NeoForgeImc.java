package li.cil.oc2.platform;

import li.cil.oc2.api.API;
import li.cil.oc2.common.integration.IMC;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;

/** Feeds NeoForge inter-mod messages to the shared {@link IMC} handler. */
@EventBusSubscriber(modid = API.MOD_ID)
public final class NeoForgeImc {
    @SubscribeEvent
    public static void handleIMCMessages(final InterModProcessEvent event) {
        IMC.handleMessages(event.getIMCStream().map(
                message -> new IMC.Message(message.senderModId(), message.method(), message.messageSupplier())));
    }

    private NeoForgeImc() {
    }
}
