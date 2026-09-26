package li.cil.oc2.platform;

import java.util.function.Supplier;

/**
 * Loader-independent entry point for registering game content.
 *
 * <p>Each mod loader module (NeoForge, later Fabric) provides one implementation, discovered through
 * {@link java.util.ServiceLoader} via {@link Platform#registries()}. Registries are addressed by their
 * resource-location string (for example {@code "minecraft:block_type"}) so this module stays free of
 * Minecraft and loader types.
 */
public interface RegistryBridge {
    /**
     * Queues {@code factory} for registration under {@code namespace:name} in the registry identified
     * by {@code registryId}.
     *
     * @return a supplier that yields the registered value once the loader has completed registration
     */
    <T> Supplier<T> register(String registryId, String namespace, String name, Supplier<? extends T> factory);
}
