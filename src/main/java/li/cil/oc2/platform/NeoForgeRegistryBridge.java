package li.cil.oc2.platform;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** {@link RegistryBridge} backed by NeoForge {@link DeferredRegister}s, one per (registry, namespace). */
public final class NeoForgeRegistryBridge implements RegistryBridge {
    private final Map<String, DeferredRegister<Object>> registers = new ConcurrentHashMap<>();
    private final Set<String> bound = ConcurrentHashMap.newKeySet();
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

    /** Attaches every register created so far to {@code modBus}; safe to call repeatedly. */
    public void bind(final IEventBus modBus) {
        lock.lock();
        try {
            registers.forEach((key, register) -> {
                if (bound.add(key)) {
                    register.register(modBus);
                }
            });
        } finally {
            lock.unlock();
        }
    }
}
