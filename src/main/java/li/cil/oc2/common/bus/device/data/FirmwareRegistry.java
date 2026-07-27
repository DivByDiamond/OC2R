package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.util.Registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.stream.Stream;

import javax.annotation.Nullable;

public final class FirmwareRegistry {
    private static final DeferredRegister<Firmware> INITIALIZER =
            DeferredRegister.create(Registries.FIRMWARE, API.MOD_ID);

    private static final Registry<Firmware> REGISTRY = INITIALIZER.makeRegistry(builder -> {});

    public static final DeferredHolder<Firmware, MinuxFirmware> MINUX =
            INITIALIZER.register("minux", MinuxFirmware::new);

    public static void initialize(IEventBus modBus) {
        INITIALIZER.register(modBus);
    }

    @SuppressWarnings("unused")
    public static ResourceLocation getKey(final Firmware firmware) {
        return INITIALIZER.getRegistryName();
    }

    @Nullable
    public static Firmware getValue(final ResourceLocation location) {
        return REGISTRY.get(location);
    }

    public static Stream<Firmware> values() {
        return REGISTRY.stream();
    }
}
