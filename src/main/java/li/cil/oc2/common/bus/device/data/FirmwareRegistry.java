package li.cil.oc2.common.bus.device.data;

import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.bus.device.data.firmware.MinuxFirmware;
import li.cil.oc2.common.bus.device.data.firmware.OnyxOSFirmware;
import li.cil.oc2.platform.NeoForgeRegistryBridge;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

public final class FirmwareRegistry {
    private static final String REGISTRY_ID = Registries.FIRMWARE.location().toString();

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

    public static void initialize(IEventBus modBus) {
        NeoForgeRegistryBridge.instance().bind(modBus);
    }

    @SuppressWarnings("unused")
    public static ResourceLocation getKey(final Firmware firmware) {
        return Registries.FIRMWARE.location();
    }

    @Nullable
    public static Firmware getValue(final ResourceLocation location) {
        return REGISTRY.get(location);
    }

    public static Stream<Firmware> values() {
        return REGISTRY.stream();
    }
}