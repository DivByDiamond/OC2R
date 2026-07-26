
package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.ProviderRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public abstract class RegistryUtils {
    private enum Phase {
        PRE_INIT,
        INIT,
        POST_INIT,
    }

    private static final List<DeferredRegister<?>> ENTRIES = new ArrayList<>();
    private static Phase phase = Phase.PRE_INIT;

    public static <T extends Registry<T>> DeferredRegister<T> getInitializerFor(final ResourceKey<Registry<T>> key) {
        if (phase != Phase.INIT) throw new IllegalStateException();

        final DeferredRegister<T> entry = DeferredRegister.create(key, API.MOD_ID);
        ENTRIES.add(entry);
        return entry;
    }

    public static <T extends Registry<T>> DeferredRegister<T> getInitializerFor(final Registry<T> registry) {
        if (phase != Phase.INIT) throw new IllegalStateException();

        final DeferredRegister<T> entry = DeferredRegister.create(registry, API.MOD_ID);
        ENTRIES.add(entry);
        return entry;
    }

    public static void begin() {
        if (phase != Phase.PRE_INIT) throw new IllegalStateException();
        phase = Phase.INIT;
    }

    public static void finish(IEventBus modBus) {
        if (phase != Phase.INIT) throw new IllegalStateException();
        phase = Phase.POST_INIT;

        for (final DeferredRegister<?> register : ENTRIES) {
            register.register(modBus);
        }

        ENTRIES.clear();
    }

    public static <T> String key(final DeviceType registryEntry) {
        return Objects.requireNonNull(registryEntry.getName()).toString();
    }

    public static <T> Optional<String> optionalKey(@Nullable final T registryEntry) {
        if(registryEntry == null) {
            return Optional.empty();
        }
        ResourceLocation providerKey = null;
        if (registryEntry instanceof final BlockDeviceProvider blockDeviceProvider) {
            providerKey = ProviderRegistry.BLOCK_DEVICE_PROVIDER_REGISTRY.getKey(blockDeviceProvider);
        } else if (registryEntry instanceof final ItemDeviceProvider itemDeviceProvider) {
            providerKey = ProviderRegistry.ITEM_DEVICE_PROVIDER_REGISTRY.getKey(itemDeviceProvider);
        }

        if (providerKey == null) {
            return Optional.empty();
        }

        return Optional.of(providerKey.toString());
    }

    private RegistryUtils() {
    }
}
