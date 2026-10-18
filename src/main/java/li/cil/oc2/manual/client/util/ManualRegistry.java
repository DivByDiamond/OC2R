/* SPDX-License-Identifier: MIT */

package li.cil.oc2.manual.client.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import li.cil.oc2.manual.api.util.Constants;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Ordered registry for client-only manual providers and models. */
public final class ManualRegistry<T> implements Iterable<T> {
    private final ResourceKey<Registry<T>> key;
    @SuppressWarnings("PMD.UseConcurrentHashMap") // Client-thread registry; insertion order is significant.
    private final Map<ResourceKey<T>, Supplier<T>> entries = new LinkedHashMap<>();

    ManualRegistry(final ResourceKey<Registry<T>> key) {
        this.key = key;
    }

    public Supplier<T> register(final String name, final Supplier<? extends T> factory) {
        return register(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name), factory);
    }

    public Supplier<T> register(final ResourceLocation id, final Supplier<? extends T> factory) {
        final ResourceKey<T> entryKey = ResourceKey.create(key, id);
        if (entries.containsKey(entryKey)) {
            throw new IllegalArgumentException("Duplicate manual entry: " + id);
        }
        // Items' component defaults are bound after client setup on 26.x. Keep upstream's
        // deferred construction semantics, so tab ItemStacks are created only when displayed.
        final Supplier<T> entry = com.google.common.base.Suppliers.memoize(
                () -> java.util.Objects.requireNonNull(factory.get()));
        entries.put(entryKey, entry);
        return entry;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals") // Provider ownership uses registered instance identity.
    public Optional<ResourceKey<T>> getKey(final T value) {
        return entries.entrySet().stream().filter(entry -> entry.getValue().get() == value)
                .map(Map.Entry::getKey).findFirst();
    }

    public Set<Map.Entry<ResourceKey<T>, T>> entrySet() {
        @SuppressWarnings("PMD.UseConcurrentHashMap") // Local ordered snapshot, never shared for mutation.
        final Map<ResourceKey<T>, T> resolved = new LinkedHashMap<>();
        entries.forEach((id, entry) -> resolved.put(id, entry.get()));
        return java.util.Collections.unmodifiableMap(resolved).entrySet();
    }

    @Override
    public Iterator<T> iterator() {
        return entries.values().stream().map(Supplier::get).iterator();
    }
}
