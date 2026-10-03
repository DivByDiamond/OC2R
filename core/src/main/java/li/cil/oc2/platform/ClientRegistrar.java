package li.cil.oc2.platform;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Loader-independent registration of client-side content (renderers, model layers, colour
 * handlers). Mod client setup describes everything it needs through this interface once; each loader
 * module applies the registrations from its own client setup hooks.
 *
 * <p>Registry entries are passed as {@link Supplier}s because the call happens during mod
 * construction, before the loader has filled the registries; the loader resolves them when it
 * applies the registration.
 */
public interface ClientRegistrar {
    /** Renders block entities of {@code type} with {@code provider}. */
    <T extends BlockEntity> void registerBlockEntityRenderer(
            Supplier<? extends BlockEntityType<? extends T>> type, BlockEntityRendererProvider<T> provider);

    /** Renders entities of {@code type} with {@code provider}. */
    <T extends Entity> void registerEntityRenderer(
            Supplier<? extends EntityType<? extends T>> type, EntityRendererProvider<T> provider);

    /** Defines the model layer {@code location}. */
    void registerLayerDefinition(ModelLayerLocation location, Supplier<LayerDefinition> definition);

    /** Tints {@code blocks} with {@code color}. */
    void registerBlockColor(BlockColor color, List<? extends Supplier<? extends Block>> blocks);

    /** Tints {@code items} with {@code color}. */
    void registerItemColor(ItemColor color, List<? extends Supplier<? extends Item>> items);

    /** Runs {@code task} on the main thread once client setup has completed. */
    void enqueueSetup(Runnable task);
}
