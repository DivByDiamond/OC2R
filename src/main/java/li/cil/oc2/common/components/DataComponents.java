package li.cil.oc2.common.components;

import li.cil.oc2.api.API;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, API.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RestrictedContainer>>
            RESTRICTED_CONTAINER =
                    COMPONENTS.registerComponentType(
                            "restricted_container",
                            builder -> builder.persistent(RestrictedContainer.CODEC));

    public static void initialize(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}