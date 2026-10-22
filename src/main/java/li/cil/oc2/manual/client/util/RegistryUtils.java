/* SPDX-License-Identifier: MIT */

package li.cil.oc2.manual.client.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Client-local manual registries, independent of loader and network registry lifecycles. */
public final class RegistryUtils {
    @SuppressWarnings("PMD.UseConcurrentHashMap") // Client-thread setup only.
    private static final Map<ResourceKey<?>, ManualRegistry<?>> REGISTRIES = new HashMap<>();

    public static ResourceLocation id(final ResourceKey<?> key) {
        //? if >=26.1 {
        /*return key.identifier();
        *///?} else {
        return key.location();
        //?}
    }

    @SuppressWarnings("unchecked")
    public static <T> ManualRegistry<T> get(final ResourceKey<Registry<T>> key) {
        return (ManualRegistry<T>) REGISTRIES.computeIfAbsent(key, unused -> new ManualRegistry<>(key));
    }

    public static <T> Supplier<T> register(final String registry, final String namespace,
                                         final String name, final Supplier<? extends T> factory) {
        final ResourceKey<Registry<T>> key = ResourceKey.createRegistryKey(ResourceLocation.parse(registry));
        return get(key).register(ResourceLocation.fromNamespaceAndPath(namespace, name), factory);
    }

    private RegistryUtils() {}
}
