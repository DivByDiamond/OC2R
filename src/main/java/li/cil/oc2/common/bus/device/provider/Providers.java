
package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import net.minecraft.core.Registry;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class Providers {
    public static Registry<BlockDeviceProvider> blockDeviceProviderRegistry() {
        return ProviderRegistry.BLOCK_DEVICE_PROVIDER_REGISTRY;
    }

    public static Registry<ItemDeviceProvider> itemDeviceProviderRegistry() {
        return ProviderRegistry.ITEM_DEVICE_PROVIDER_REGISTRY;
    }

    public static void registerBlockDeviceProviders(final BiConsumer<String, Supplier<BlockDeviceProvider>> registry) {

    }

    public static void registerItemDeviceProviders(final BiConsumer<String, Supplier<ItemDeviceProvider>> registry) {

    }
}
