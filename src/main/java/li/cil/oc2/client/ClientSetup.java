/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.screen.KeyboardScreen;
import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.client.item.CustomItemModelProperties;
import li.cil.oc2.client.model.BusCableModelLoader;
import li.cil.oc2.client.renderer.BusInterfaceNameRenderer;
import li.cil.oc2.client.renderer.blockentity.*;
import li.cil.oc2.client.renderer.color.BusCableBlockColor;
import li.cil.oc2.client.renderer.entity.RobotRenderer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.entity.Entities;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = API.MOD_ID, value = {Dist.CLIENT})
public final class ClientSetup {
    @Nullable private static Boolean captureInputState = null;

    @SubscribeEvent
    public static void handleSetupEvent(final FMLClientSetupEvent event) {
        BusInterfaceNameRenderer.initialize();

        BlockEntityRenderers.register(BlockEntities.COMPUTER.get(), ComputerRenderer::new);
        BlockEntityRenderers.register(BlockEntities.MONITOR.get(), MonitorRenderer::new);
        BlockEntityRenderers.register(BlockEntities.DISK_DRIVE.get(), DiskDriveRenderer::new);
        BlockEntityRenderers.register(BlockEntities.CHARGER.get(), ChargerRenderer::new);
        BlockEntityRenderers.register(BlockEntities.PROJECTOR.get(), ProjectorRenderer::new);
        BlockEntityRenderers.register(BlockEntities.INTERNET_GATEWAY.get(), InternetGateWayRenderer::new);

        event.enqueueWork(() -> {
            CustomItemModelProperties.initialize();
            CustomItemColors.initialize();
        });
    }

    @SubscribeEvent
    public static void handleModelRegistryEvent(final RegisterGeometryLoaders event) {
        if (Blocks.BUS_CABLE.getId() == null) throw new RuntimeException("Null bus cable ID");
        event.register(Blocks.BUS_CABLE.getId(), new BusCableModelLoader());
    }

    @SubscribeEvent
    public static void renderHotbar(RenderGuiLayerEvent.Pre event) {
        if (event.getName() == VanillaGuiLayers.HOTBAR && KeyboardScreen.hideHotbar) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void handleColorHandler(final RegisterColorHandlersEvent.Block event) {
        event.register(new BusCableBlockColor(), Blocks.BUS_CABLE.get());
    }

    @SubscribeEvent
    public static void handleEntityRendererRegisterEvent(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Entities.ROBOT.get(), RobotRenderer::new);
    }

    @SubscribeEvent
    public static void handleRegisterLayerDefinitionsEvent(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);
    }

    public static boolean getCaptureInputState() {
        if (captureInputState == null) {
            captureInputState = Config.captureInputDefaultState;
        }

        return captureInputState;
    }

    public static void setCaptureInputState(final boolean value) {
        captureInputState = value;
    }
}
