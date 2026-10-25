package li.cil.oc2.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** {@link RegistryBridge} backed by NeoForge {@link DeferredRegister}s, one per (registry, namespace). */
public final class NeoForgeRegistryBridge implements RegistryBridge {
    private final Map<String, DeferredRegister<Object>> registers = new ConcurrentHashMap<>();
    private final Set<String> bound = ConcurrentHashMap.newKeySet();
    private final Map<String, DeferredRegister.Blocks> blockRegisters = new ConcurrentHashMap<>();
    private final Map<String, DeferredRegister.Items> itemRegisters = new ConcurrentHashMap<>();
    private final Map<String, List<BlockHolder<?>>> blockHolders = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    /** Returns the instance the {@link Platform} service loader hands out. */
    public static NeoForgeRegistryBridge instance() {
        return (NeoForgeRegistryBridge) Platform.registries();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Supplier<T> register(
            final String registryId, final String namespace, final String name, final Supplier<? extends T> factory) {
        final String key = registryId + '|' + namespace;
        lock.lock();
        try {
            final DeferredRegister<Object> register = registers.computeIfAbsent(key,
                    k -> DeferredRegister.create(ResourceLocation.parse(registryId), namespace));
            return (Supplier<T>) register.register(name, (Supplier) factory);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <B extends Block> BlockHolder<B> registerBlock(
            final String namespace, final String name, final Supplier<B> factory) {
        lock.lock();
        try {
            final DeferredBlock<B> deferred = blockRegisters
                    .computeIfAbsent(namespace, DeferredRegister::createBlocks)
                    .register(name, () -> RegistrationContext.with(
                            ResourceLocation.fromNamespaceAndPath(namespace, name), factory));
            final BlockHolder<B> holder = new BlockHolder<>() {
                @Override
                public ResourceLocation getId() {
                    return deferred.getId();
                }

                @Override
                public B get() {
                    return deferred.get();
                }
            };
            blockHolders.computeIfAbsent(namespace, k -> new ArrayList<>()).add(holder);
            return holder;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <I extends Item> ItemHolder<I> registerItem(
            final String namespace, final String name, final Supplier<I> factory) {
        lock.lock();
        try {
            final DeferredItem<I> deferred = itemRegisters
                    .computeIfAbsent(namespace, DeferredRegister::createItems)
                    .register(name, () -> RegistrationContext.with(
                            ResourceLocation.fromNamespaceAndPath(namespace, name), factory));
            return new ItemHolder<>() {
                @Override
                public ResourceLocation getId() {
                    return deferred.getId();
                }

                @Override
                public I get() {
                    return deferred.get();
                }
            };
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Registry<T> createRegistry(final String registryId, final String namespace, final boolean synced) {
        final String key = registryId + '|' + namespace;
        lock.lock();
        try {
            final DeferredRegister<Object> register = registers.computeIfAbsent(key,
                    k -> DeferredRegister.create(ResourceLocation.parse(registryId), namespace));
            return (Registry<T>) register.makeRegistry(builder -> builder.sync(synced));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addItemAlias(final String namespace, final ResourceLocation oldId, final ResourceLocation target) {
        lock.lock();
        try {
            itemRegisters.computeIfAbsent(namespace, DeferredRegister::createItems).addAlias(oldId, target);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("ConstantConditions") // The legacy vanilla builder accepts a null datafixer type.
    @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION",
            justification = "The legacy builder accepts null, as used by vanilla block entity types")
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(
            final BiFunction<BlockPos, BlockState, T> factory, final Block... blocks) {
        //? if >=26.1 {
        /*return new BlockEntityType<>(factory::apply, blocks);
        *///?} else {
        return BlockEntityType.Builder.of(factory::apply, blocks).build(null);
        //?}
    }

    @Override
    public List<BlockHolder<?>> blocks(final String namespace) {
        lock.lock();
        try {
            // Copy under the lock: registerBlock appends to the same list while it is being read.
            return List.copyOf(blockHolders.getOrDefault(namespace, List.of()));
        } finally {
            lock.unlock();
        }
    }

    /** Attaches every register created so far to {@code modBus}; safe to call repeatedly. */
    public void bind(final IEventBus modBus) {
        lock.lock();
        try {
            registers.forEach((key, register) -> {
                if (bound.add(key)) {
                    register.register(modBus);
                }
            });
            // Prefixed with a character ResourceLocation namespaces/paths can never contain, so
            // these can never collide with a `registryId + '|' + namespace` key from `registers`
            // (for example a caller mistakenly passing registryId "minecraft:block" to register()
            // instead of using registerBlock() would otherwise land on the same "block|<ns>" key).
            blockRegisters.forEach((ns, register) -> {
                if (bound.add("\u0000block|" + ns)) {
                    register.register(modBus);
                }
            });
            itemRegisters.forEach((ns, register) -> {
                if (bound.add("\u0000item|" + ns)) {
                    register.register(modBus);
                }
            });
        } finally {
            lock.unlock();
        }
    }
}
