package li.cil.oc2.common.bus.device.data;

import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.bus.device.data.firmware.MinuxFirmware;
import li.cil.oc2.common.bus.device.data.firmware.OnyxOSFirmware;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public final class FirmwareRegistry {
    //? if >=26.1 {
    /*private static final String REGISTRY_ID = Registries.FIRMWARE.identifier().toString();
    *///?} else {
    private static final String REGISTRY_ID = Registries.FIRMWARE.location().toString();
    //?}

    private static final Registry<Firmware> REGISTRY =
            Platform.registries().createRegistry(REGISTRY_ID, API.MOD_ID);

    public static final ResourceLocation MINUX_ID =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "minux");
    public static final ResourceLocation ONYXOS_ID =
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "onyxos");

    public static final Supplier<MinuxFirmware> MINUX =
            Platform.registries().register(REGISTRY_ID, API.MOD_ID, "minux", MinuxFirmware::new);

    public static final Supplier<OnyxOSFirmware> ONYXOS =
            Platform.registries().register(REGISTRY_ID, API.MOD_ID, "onyxos", OnyxOSFirmware::new);

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }

    @Nullable
    public static ResourceLocation getKey(final Firmware firmware) {
        return REGISTRY.getKey(firmware);
    }

    @Nullable
    public static Firmware getValue(final ResourceLocation location) {
        //? if >=26.1 {
        /*return REGISTRY.getValue(location);
        *///?} else {
        return REGISTRY.get(location);
        //?}
    }

    public static Stream<Firmware> values() {
        return REGISTRY.stream();
    }
}