package li.cil.oc2.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Client half of {@link FabricBlockEntityHooks}; separate so servers never load client classes. */
@Environment(EnvType.CLIENT)
public final class FabricClientBlockEntityHooks {
    private FabricClientBlockEntityHooks() {
    }

    public static void register() {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(
                (blockEntity, level) -> FabricBlockEntityHooks.enqueue(blockEntity, level));
        ClientTickEvents.START_WORLD_TICK.register(FabricBlockEntityHooks::drain);
    }
}
