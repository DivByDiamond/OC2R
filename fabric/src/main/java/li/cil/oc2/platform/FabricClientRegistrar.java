package li.cil.oc2.platform;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * {@link ClientRegistrar} for Fabric. Registry entries are resolved immediately, so this must run
 * from the client initializer, after {@link FabricRegistryBridge#bind()}. Only loaded on the client.
 */
@Environment(EnvType.CLIENT)
public final class FabricClientRegistrar implements ClientRegistrar {
    @Override
    public <T extends BlockEntity> void registerBlockEntityRenderer(
            final Supplier<? extends BlockEntityType<? extends T>> type,
            final BlockEntityRendererProvider<T> provider) {
        BlockEntityRenderers.register(type.get(), provider);
    }

    @Override
    public <T extends Entity> void registerEntityRenderer(
            final Supplier<? extends EntityType<? extends T>> type, final EntityRendererProvider<T> provider) {
        EntityRendererRegistry.register(type.get(), provider);
    }

    @Override
    public void registerLayerDefinition(
            final ModelLayerLocation location, final Supplier<LayerDefinition> definition) {
        EntityModelLayerRegistry.registerModelLayer(location, definition::get);
    }

    @Override
    public void registerBlockColor(
            final BlockColor color, final List<? extends Supplier<? extends Block>> blocks) {
        ColorProviderRegistry.BLOCK.register(
                color, blocks.stream().map(Supplier::get).toArray(Block[]::new));
    }

    @Override
    public void registerItemColor(
            final ItemColor color, final List<? extends Supplier<? extends Item>> items) {
        ColorProviderRegistry.ITEM.register(
                color, items.stream().map(Supplier::get).toArray(Item[]::new));
    }

    @Override
    public void enqueueSetup(final Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}
