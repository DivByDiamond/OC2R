package li.cil.oc2.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * {@link RegistryBridge} for Fabric. Fabric registries are immediate, so registrations are queued
 * and flushed in queue order by {@link #bind()} from the mod initializer; queue order is the order
 * the common code initialises its registries in (blocks before items before block entities), which
 * is what makes factories that read earlier holders (a block item reading its block) work.
 */
public final class FabricRegistryBridge implements RegistryBridge {
    private final List<Entry<?>> queue = new ArrayList<>();
    private final Map<String, Registry<?>> customRegistries = new ConcurrentHashMap<>();
    private final Map<String, List<BlockHolder<?>>> blockHolders = new ConcurrentHashMap<>();
    private final List<Alias> aliases = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private boolean bound;

    /** Returns the instance the {@link Platform} service loader hands out. */
    public static FabricRegistryBridge instance() {
        return (FabricRegistryBridge) Platform.registries();
    }

    @Override
    public <T> Supplier<T> register(
            final String registryId, final String namespace, final String name, final Supplier<? extends T> factory) {
        final Entry<T> entry = new Entry<>(registryId, namespace, name, factory);
        enqueue(entry);
        return entry;
    }

    @Override
    public <B extends Block> BlockHolder<B> registerBlock(
            final String namespace, final String name, final Supplier<B> factory) {
        final Entry<B> entry = new Entry<>("minecraft:block", namespace, name, factory);
        final BlockHolder<B> holder = new BlockHolder<>() {
            @Override
            public ResourceLocation getId() {
                return entry.id();
            }

            @Override
            public B get() {
                return entry.get();
            }
        };
        lock.lock();
        try {
            enqueueLocked(entry);
            blockHolders.computeIfAbsent(namespace, k -> new ArrayList<>()).add(holder);
        } finally {
            lock.unlock();
        }
        return holder;
    }

    @Override
    public <I extends Item> ItemHolder<I> registerItem(
            final String namespace, final String name, final Supplier<I> factory) {
        final Entry<I> entry = new Entry<>("minecraft:item", namespace, name, factory);
        enqueue(entry);
        return new ItemHolder<>() {
            @Override
            public ResourceLocation getId() {
                return entry.id();
            }

            @Override
            public I get() {
                return entry.get();
            }
        };
    }

    @Override
    public List<BlockHolder<?>> blocks(final String namespace) {
        lock.lock();
        try {
            return List.copyOf(blockHolders.getOrDefault(namespace, List.of()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addItemAlias(final String namespace, final ResourceLocation oldId, final ResourceLocation target) {
        lock.lock();
        try {
            aliases.add(new Alias(oldId, target));
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Registry<T> createRegistry(final String registryId, final String namespace, final boolean synced) {
        return (Registry<T>) customRegistries.computeIfAbsent(registryId, id -> {
            final FabricRegistryBuilder<T, ? extends MappedRegistry<T>> builder =
                    FabricRegistryBuilder.createSimple(ResourceKey.<T>createRegistryKey(ResourceLocation.parse(id)));
            if (synced) {
                builder.attribute(RegistryAttribute.SYNCED);
            }
            return builder.buildAndRegister();
        });
    }

    /** Registers everything queued so far, in queue order; called once from the mod initializer. */
    public void bind() {
        lock.lock();
        try {
            if (bound) {
                throw new IllegalStateException("Registries already bound");
            }
            bound = true;
            for (final Entry<?> entry : queue) {
                entry.registerInto(resolve(entry.registryId));
            }
            queue.clear();
            for (final Alias alias : aliases) {
                ((MappedRegistry<Item>) BuiltInRegistries.ITEM).addAlias(alias.oldId(), alias.target());
            }
            aliases.clear();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private Registry<Object> resolve(final String registryId) {
        final Registry<?> custom = customRegistries.get(registryId);
        if (custom != null) {
            return (Registry<Object>) custom;
        }
        final Registry<?> builtIn = BuiltInRegistries.REGISTRY.get(ResourceLocation.parse(registryId));
        if (builtIn == null) {
            throw new IllegalStateException("Unknown registry " + registryId);
        }
        return (Registry<Object>) builtIn;
    }

    private void enqueue(final Entry<?> entry) {
        lock.lock();
        try {
            enqueueLocked(entry);
        } finally {
            lock.unlock();
        }
    }

    private void enqueueLocked(final Entry<?> entry) {
        if (bound) {
            throw new IllegalStateException(
                    "Registration of " + entry.id() + " after registries were bound");
        }
        queue.add(entry);
    }

    private record Alias(ResourceLocation oldId, ResourceLocation target) {}

    private static final class Entry<T> implements Supplier<T> {
        private final String registryId;
        private final ResourceLocation id;
        private final Supplier<? extends T> factory;
        private volatile T value;

        Entry(final String registryId, final String namespace, final String name, final Supplier<? extends T> factory) {
            this.registryId = registryId;
            this.id = ResourceLocation.fromNamespaceAndPath(namespace, name);
            this.factory = factory;
        }

        ResourceLocation id() {
            return id;
        }

        void registerInto(final Registry<Object> registry) {
            final T created = factory.get();
            Registry.register(registry, id, created);
            value = created;
        }

        @Override
        public T get() {
            final T result = value;
            if (result == null) {
                throw new IllegalStateException("Registry entry " + id + " is not registered yet");
            }
            return result;
        }
    }
}
