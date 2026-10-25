package li.cil.oc2.platform;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
*///?}
//? if <26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
//?}
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
*///?}
import net.minecraft.client.Minecraft;
//? if <26.1 {
import net.minecraft.client.color.block.BlockColor;
//?}
//? if >=26.1 {
/*import net.minecraft.client.color.block.BlockTintSource;
*///?}
//? if <26.1 {
import net.minecraft.client.color.item.ItemColor;
//?}
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
//? if >=26.1 {
/*import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
*///?}
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
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
    //? if >=26.1 {
/*    public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
            final Supplier<? extends BlockEntityType<? extends T>> type,
            final BlockEntityRendererProvider<T, S> provider) {
*///?} else {
    public <T extends BlockEntity> void registerBlockEntityRenderer(
            final Supplier<? extends BlockEntityType<? extends T>> type,
            final BlockEntityRendererProvider<T> provider) {
        //?}
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
        //? if >=26.1 {
/*        ModelLayerRegistry.registerModelLayer(location, definition::get);
*///?} else {
        EntityModelLayerRegistry.registerModelLayer(location, definition::get);
        //?}
    }

    //? if >=26.1 {
/*    @Override
    public void registerBlockColor(
            final Object color, final List<? extends Supplier<? extends Block>> blocks) {
        if (color instanceof final BlockTintSource tintSource) {
            BlockColorRegistry.register(List.of(tintSource), blocks.stream().map(Supplier::get).toArray(Block[]::new));
        }
    }

    @Override
    public void registerItemColor(
            final Object color, final List<? extends Supplier<? extends Item>> items) {
        // Modern item colors are supplied by the item definition's tint sources.
    }
*///?} else {
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

    //?}

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreen(
            final Supplier<? extends MenuType<M>> type, final ScreenFactory<M, S> constructor) {
        MenuScreens.register(type.get(), constructor::create);
    }

    @Override
    public void enqueueSetup(final Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}
