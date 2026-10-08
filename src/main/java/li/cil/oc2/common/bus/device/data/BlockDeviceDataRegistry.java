package li.cil.oc2.common.bus.device.data;

import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.bus.device.data.block.BuildrootBlockDeviceData;
import li.cil.oc2.common.bus.device.data.block.OnyxOSBlockDeviceData;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("unused")
public final class BlockDeviceDataRegistry {
    //? if >=26.1 {
    /*private static final String REGISTRY_ID = Registries.BLOCK_DEVICE_DATA.identifier().toString();
    *///?} else {
    private static final String REGISTRY_ID = Registries.BLOCK_DEVICE_DATA.location().toString();
    //?}

    private static final Registry<BlockDeviceData> REGISTRY =
            Platform.registries().createRegistry(REGISTRY_ID, API.MOD_ID);

    public static final Supplier<BuildrootBlockDeviceData> BUILDROOT =
            Platform.registries().register(
                    REGISTRY_ID, API.MOD_ID, "buildroot", BuildrootBlockDeviceData::new);

    public static final Supplier<OnyxOSBlockDeviceData> ONYXOS =
            Platform.registries().register(
                    REGISTRY_ID, API.MOD_ID, "onyxos-base", OnyxOSBlockDeviceData::new);

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }

    @Nullable
    public static ResourceLocation getKey(final BlockDeviceData data) {
        ResourceLocation location = REGISTRY.getKey(data);
        if (location == null) {
            location = FileSystems.getKeyByValue(data);
        }
        return location;
    }

    @Nullable
    public static BlockDeviceData getValue(final ResourceLocation location) {
        //? if >=26.1 {
        /*final BlockDeviceData value = REGISTRY.getValue(location);
        *///?} else {
        final BlockDeviceData value = REGISTRY.get(location);
        //?}
        if (value != null) {
            return value;
        }
        return FileSystems.getBlockData().get(location);
    }

    public static Stream<BlockDeviceData> values() {
        return Stream.concat(REGISTRY.stream(), FileSystems.getBlockData().values().stream());
    }
}