package li.cil.oc2.platform;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

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

    /** Queues a block for registration under {@code namespace:name}. */
    <B extends Block> BlockHolder<B> registerBlock(String namespace, String name, Supplier<B> factory);

    /** Queues an item for registration under {@code namespace:name}. */
    <I extends Item> ItemHolder<I> registerItem(String namespace, String name, Supplier<I> factory);

    /** All blocks registered through {@link #registerBlock} for {@code namespace}, in registration order. */
    List<BlockHolder<?>> blocks(String namespace);

    /** Makes {@code oldId} resolve to {@code target} when loading data that still uses the old item id. */
    void addItemAlias(String namespace, ResourceLocation oldId, ResourceLocation target);
}
