package li.cil.oc2.common.components;

import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.component.DataComponentType;

public class DataComponents {
    public static final Supplier<DataComponentType<RestrictedContainer>> RESTRICTED_CONTAINER =
            Platform.registries().register(
                    "minecraft:data_component_type",
                    API.MOD_ID,
                    "restricted_container",
                    () -> DataComponentType.<RestrictedContainer>builder()
                            .persistent(RestrictedContainer.CODEC)
                            .build());

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }
}