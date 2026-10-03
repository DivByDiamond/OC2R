package li.cil.oc2.common.util;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.provider.ProviderRegistry;
import net.minecraft.resources.ResourceLocation;

public abstract class RegistryUtils {
    public static String key(final DeviceType registryEntry) {
        return Objects.requireNonNull(registryEntry.getName()).toString();
    }

    public static <T> Optional<String> optionalKey(@Nullable final T registryEntry) {
        if (registryEntry == null) {
            return Optional.empty();
        }
        ResourceLocation providerKey = null;
        if (registryEntry instanceof final BlockDeviceProvider blockDeviceProvider) {
            providerKey =
                    ProviderRegistry.BLOCK_DEVICE_PROVIDER_REGISTRY.getKey(blockDeviceProvider);
        } else if (registryEntry instanceof final ItemDeviceProvider itemDeviceProvider) {
            providerKey = ProviderRegistry.ITEM_DEVICE_PROVIDER_REGISTRY.getKey(itemDeviceProvider);
        }

        if (providerKey == null) {
            return Optional.empty();
        }

        return Optional.of(providerKey.toString());
    }

    private RegistryUtils() {}
}