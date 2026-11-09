package li.cil.oc2.common.components;

import java.util.UUID;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.UUIDUtil;
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

    /** Identity of a tablet; its machine state is stored in world data under this id. */
    public static final Supplier<DataComponentType<UUID>> TABLET_ID =
            Platform.registries().register(
                    "minecraft:data_component_type",
                    API.MOD_ID,
                    "tablet_id",
                    () -> DataComponentType.<UUID>builder()
                            .persistent(UUIDUtil.CODEC)
                            .networkSynchronized(UUIDUtil.STREAM_CODEC)
                            .build());

    /**
     * Current radio secret of a tablet, rotated on every activation. It is saved with the item but
     * never sent to clients: the network codec carries nothing.
     */
    public static final Supplier<DataComponentType<String>> TABLET_RADIO_SECRET =
            Platform.registries().register(
                    "minecraft:data_component_type",
                    API.MOD_ID,
                    "tablet_radio_secret",
                    () -> DataComponentType.<String>builder()
                            .persistent(com.mojang.serialization.Codec.STRING)
                            .networkSynchronized(net.minecraft.network.codec.StreamCodec.unit(""))
                            .build());

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }
}