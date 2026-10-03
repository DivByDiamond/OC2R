package li.cil.oc2.platform;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Loader-independent entry point for registering game content.
 *
 * <p>Each mod loader module (NeoForge, later Fabric) provides one implementation, discovered through
 * {@link java.util.ServiceLoader} via {@link Platform#registries()}. The module is free of <em>loader</em>
 * types, not of Minecraft itself: {@code core} compiles against vanilla sources through NeoForm, so
 * this interface may type {@link Block}, {@link Item}, {@link Registry} and {@link ResourceLocation}.
 * The generic {@link #register} and {@link #createRegistry} address their registry by string id (for
 * example {@code "minecraft:block_entity_type"}), the typed helpers below use the Minecraft type
 * itself.
 */
public interface RegistryBridge {
    /**
     * Queues {@code factory} for registration under {@code namespace:name} in the registry identified
     * by {@code registryId}.
     *
     * @return a supplier that yields the registered value once the loader has completed registration
     */
    <T> Supplier<T> register(String registryId, String namespace, String name, Supplier<? extends T> factory);

    /** Queues a block for registration under {@code namespace:name}. */
    <B extends Block> BlockHolder<B> registerBlock(String namespace, String name, Supplier<B> factory);

    /** Queues an item for registration under {@code namespace:name}. */
    <I extends Item> ItemHolder<I> registerItem(String namespace, String name, Supplier<I> factory);

    /** All blocks registered through {@link #registerBlock} for {@code namespace}, in registration order. */
    List<BlockHolder<?>> blocks(String namespace);

    /** Makes {@code oldId} resolve to {@code target} when loading data that still uses the old item id. */
    void addItemAlias(String namespace, ResourceLocation oldId, ResourceLocation target);

    /**
     * Creates a new custom registry {@code registryId} owned by {@code namespace}. Must be called before
     * entries are registered into it through {@link #register} and before the loader binds registries.
     */
    default <T> Registry<T> createRegistry(final String registryId, final String namespace) {
        return createRegistry(registryId, namespace, false);
    }

    /**
     * Same as {@link #createRegistry(String, String)}; a {@code synced} registry has its content sent to
     * connecting clients, which is needed for registries that ids are exchanged over the network for.
     */
    <T> Registry<T> createRegistry(String registryId, String namespace, boolean synced);
}
