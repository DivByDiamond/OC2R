package li.cil.oc2.client;

import java.util.List;
import li.cil.oc2.client.gui.screen.keyboard.KeyboardScreen;
import li.cil.oc2.client.item.CustomItemColors;
import li.cil.oc2.client.item.CustomItemModelProperties;
import li.cil.oc2.client.renderer.BusInterfaceNameRenderer;
import li.cil.oc2.client.renderer.blockentity.charger.ChargerRenderer;
import li.cil.oc2.client.renderer.blockentity.computer.ComputerRenderer;
import li.cil.oc2.client.renderer.blockentity.computer.DiskDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.monitor.MonitorRenderer;
import li.cil.oc2.client.renderer.blockentity.network.InternetGateWayRenderer;
import li.cil.oc2.client.renderer.blockentity.projector.ProjectorRenderer;
import li.cil.oc2.client.renderer.color.BusCableBlockColor;
import li.cil.oc2.client.renderer.entity.RobotRenderer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.block.common.Blocks;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.platform.ClientRegistrar;
import li.cil.oc2.platform.event.ClientEvents;
import org.jetbrains.annotations.Nullable;

public final class ClientSetup {
    @Nullable private static Boolean captureInputState = null;

    private ClientSetup() {}

    /** Describes all client registrations; the loader module applies them from its setup hooks. */
    public static void register(final ClientRegistrar registrar) {
        registrar.enqueueSetup(BusInterfaceNameRenderer::initialize);
        registrar.enqueueSetup(CustomItemModelProperties::initialize);

        registrar.registerBlockEntityRenderer(BlockEntities.COMPUTER, ComputerRenderer::new);
        registrar.registerBlockEntityRenderer(BlockEntities.MONITOR, MonitorRenderer::new);
        registrar.registerBlockEntityRenderer(BlockEntities.DISK_DRIVE, DiskDriveRenderer::new);
        registrar.registerBlockEntityRenderer(BlockEntities.CHARGER, ChargerRenderer::new);
        registrar.registerBlockEntityRenderer(BlockEntities.PROJECTOR, ProjectorRenderer::new);
        registrar.registerBlockEntityRenderer(
                BlockEntities.INTERNET_GATEWAY, InternetGateWayRenderer::new);

        registrar.registerEntityRenderer(Entities.ROBOT, RobotRenderer::new);
        registrar.registerLayerDefinition(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);

        registrar.registerBlockColor(new BusCableBlockColor(), List.of(Blocks.BUS_CABLE));
        CustomItemColors.initialize(registrar);

        ClientEvents.HIDE_HOTBAR.register(() -> KeyboardScreen.hideHotbar);
    }

    /**
     * Gets the capture input state.
     *
     * @return the capture input state.
     */
    public static boolean getCaptureInputState() { // NOPMD getter API consumed across client/server
        if (captureInputState == null) {
            captureInputState = Config.captureInputDefaultState;
        }

        return captureInputState;
    }

    /**
     * Sets the capture input state.
     *
     * @param value the new capture input state.
     */
    public static void setCaptureInputState(final boolean value) {
        captureInputState = value;
    }
}