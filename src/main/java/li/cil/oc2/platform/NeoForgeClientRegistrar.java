package li.cil.oc2.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * {@link ClientRegistrar} for NeoForge. NeoForge only offers each registration point during its own
 * mod bus event, so registrations are queued when mod construction describes them and applied by
 * {@link NeoForgeClientSetup} when the matching event fires.
 */
public final class NeoForgeClientRegistrar implements ClientRegistrar {
    private static final NeoForgeClientRegistrar SHARED = new NeoForgeClientRegistrar();

    private final List<Runnable> blockEntityRenderers = new ArrayList<>();
    private final List<java.util.function.Consumer<EntityRenderersEvent.RegisterRenderers>> entityRenderers =
            new ArrayList<>();
    private final List<java.util.function.Consumer<EntityRenderersEvent.RegisterLayerDefinitions>> layers =
            new ArrayList<>();
    private final List<java.util.function.Consumer<RegisterColorHandlersEvent.Block>> blockColors =
            new ArrayList<>();
    private final List<java.util.function.Consumer<RegisterColorHandlersEvent.Item>> itemColors =
            new ArrayList<>();
    private final List<java.util.function.Consumer<RegisterMenuScreensEvent>> screens = new ArrayList<>();
    private final List<Runnable> queuedSetupTasks = new ArrayList<>();

    private NeoForgeClientRegistrar() {}

    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification =
                    "accessor for the private singleton; returning SHARED by reference is the"
                            + " point of the holder")
    public static NeoForgeClientRegistrar instance() {
        return SHARED;
    }

    @Override
    public <T extends BlockEntity> void registerBlockEntityRenderer(
            final Supplier<? extends BlockEntityType<? extends T>> type,
            final BlockEntityRendererProvider<T> provider) {
        blockEntityRenderers.add(() ->
                net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(type.get(), provider));
    }

    @Override
    public <T extends Entity> void registerEntityRenderer(
            final Supplier<? extends EntityType<? extends T>> type, final EntityRendererProvider<T> provider) {
        entityRenderers.add(event -> event.registerEntityRenderer(type.get(), provider));
    }

    @Override
    public void registerLayerDefinition(
            final ModelLayerLocation location, final Supplier<LayerDefinition> definition) {
        layers.add(event -> event.registerLayerDefinition(location, definition));
    }

    @Override
    public void registerBlockColor(
            final BlockColor color, final List<? extends Supplier<? extends Block>> blocks) {
        blockColors.add(event ->
                event.register(color, blocks.stream().map(Supplier::get).toArray(Block[]::new)));
    }

    @Override
    public void registerItemColor(
            final ItemColor color, final List<? extends Supplier<? extends Item>> items) {
        itemColors.add(event ->
                event.register(color, items.stream().map(Supplier::get).toArray(Item[]::new)));
    }

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void registerScreen(
            final Supplier<? extends MenuType<M>> type, final ScreenFactory<M, S> constructor) {
        screens.add(event -> event.register(type.get(), constructor::create));
    }

    @Override
    public void enqueueSetup(final Runnable task) {
        queuedSetupTasks.add(task);
    }

    void applyBlockEntityRenderers() {
        blockEntityRenderers.forEach(Runnable::run);
    }

    void applyEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        entityRenderers.forEach(registration -> registration.accept(event));
    }

    void applyLayers(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        layers.forEach(registration -> registration.accept(event));
    }

    void applyBlockColors(final RegisterColorHandlersEvent.Block event) {
        blockColors.forEach(registration -> registration.accept(event));
    }

    void applyItemColors(final RegisterColorHandlersEvent.Item event) {
        itemColors.forEach(registration -> registration.accept(event));
    }

    void applyScreens(final RegisterMenuScreensEvent event) {
        screens.forEach(registration -> registration.accept(event));
    }

    List<Runnable> setupTasks() {
        return queuedSetupTasks;
    }
}
