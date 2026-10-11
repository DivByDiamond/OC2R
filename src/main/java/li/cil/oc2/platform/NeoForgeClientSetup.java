package li.cil.oc2.platform;

import java.util.concurrent.atomic.AtomicBoolean;
import li.cil.oc2.api.API;
//? if >=26.1 {
/*import li.cil.oc2.client.model.BusCableModel;
import li.cil.oc2.client.model.monitor.MonitorModel;
import net.minecraft.resources.ResourceLocation;
*///?} else {
import li.cil.oc2.client.model.BusCableModelLoader;
import li.cil.oc2.client.model.monitor.MonitorModelLoader;
//?}
//? if <26.1 {
import li.cil.oc2.client.renderer.entity.RobotWithoutLevelRenderer;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.item.Items;
//?}
import li.cil.oc2.platform.event.ClientEvents;
//? if <26.1 {
import net.minecraft.client.Minecraft;
//?}
//? if <26.1 {
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
//?}
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//? if >=26.1 {
/*import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
*///?} else {
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;
//?}
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
//? if >=26.1 {
/*import li.cil.oc2.client.renderer.entity.RobotSpecialModelRenderer;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
*///?}
//? if <26.1 {
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
//?}
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Applies the registrations described through {@link ClientRegistrar} from NeoForge's client mod
 * bus events, plus the NeoForge-only geometry loader registration.
 */
@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() {}

    @SubscribeEvent
    @SuppressWarnings("FutureReturnValueIgnored")
    public static void onClientSetup(final FMLClientSetupEvent event) {
        final NeoForgeClientRegistrar registrar = NeoForgeClientRegistrar.instance();
        registrar.applyBlockEntityRenderers();
        registrar.setupTasks().forEach(event::enqueueWork);
    }

    // 26.x has neither geometry loaders nor BlockEntityWithoutLevelRenderer: the bus cable and
    // monitor register as custom block state models, the robot item rendering waits for its port.
//? if >=26.1 {
    /*@SubscribeEvent
    public static void onRegisterBlockStateModels(final RegisterBlockStateModels event) {
        event.registerModel(
                ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "bus_cable"), BusCableModel.CODEC);
        event.registerModel(
                ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "monitor"), MonitorModel.CODEC);
    }

*///?}
//? if <26.1 {
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(final RegisterGeometryLoaders event) {
        event.register(Blocks.BUS_CABLE.getId(), new BusCableModelLoader());
        event.register(Blocks.MONITOR.getId(), new MonitorModelLoader());
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(final RegisterClientExtensionsEvent event) {
        // The robot item renders its entity model instead of a flat item model.
        event.registerItem(
                new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return new RobotWithoutLevelRenderer(
                                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                                Minecraft.getInstance().getEntityModels());
                    }
                },
                Items.ROBOT.get());
    }

//?}
    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        NeoForgeClientRegistrar.instance().applyEntityRenderers(event);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(
            final EntityRenderersEvent.RegisterLayerDefinitions event) {
        NeoForgeClientRegistrar.instance().applyLayers(event);
    }

//? if >=26.1 {
/*    @SubscribeEvent
    public static void onRegisterSpecialModelRenderers(final RegisterSpecialModelRendererEvent event) {
        event.register(RobotSpecialModelRenderer.ID, RobotSpecialModelRenderer.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(final RegisterColorHandlersEvent.BlockTintSources event) {
        NeoForgeClientRegistrar.instance().applyBlockColors(event);
    }
*///?} else {
    @SubscribeEvent
    public static void onRegisterBlockColors(final RegisterColorHandlersEvent.Block event) {
        NeoForgeClientRegistrar.instance().applyBlockColors(event);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(final RegisterColorHandlersEvent.Item event) {
        NeoForgeClientRegistrar.instance().applyItemColors(event);
    }
//?}

    @SubscribeEvent
    public static void onRegisterMenuScreens(final RegisterMenuScreensEvent event) {
        NeoForgeClientRegistrar.instance().applyScreens(event);
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(final RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            return;
        }
        final AtomicBoolean hide = new AtomicBoolean();
        ClientEvents.HIDE_HOTBAR.fire(listener -> {
            if (listener.getAsBoolean()) {
                hide.set(true);
            }
        });
        if (hide.get()) {
            event.setCanceled(true);
        }
    }
}
