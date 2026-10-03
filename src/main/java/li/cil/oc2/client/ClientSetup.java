package li.cil.oc2.client;

import java.util.List;
import li.cil.oc2.client.gui.screen.computer.ComputerContainerScreen;
import li.cil.oc2.client.gui.screen.computer.ComputerTerminalScreen;
import li.cil.oc2.client.gui.screen.keyboard.KeyboardScreen;
import li.cil.oc2.client.gui.screen.monitor.MonitorDisplayScreen;
import li.cil.oc2.client.gui.screen.network.NetworkTunnelScreen;
import li.cil.oc2.client.gui.screen.robot.RobotContainerScreen;
import li.cil.oc2.client.gui.screen.robot.RobotTerminalScreen;
import li.cil.oc2.client.hooks.ClientProxyImpl;
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
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.hooks.ClientProxy;
import li.cil.oc2.platform.ClientRegistrar;
import li.cil.oc2.platform.event.ClientEvents;

public final class ClientSetup {
    private ClientSetup() {}

    /** Describes all client registrations; the loader module applies them from its setup hooks. */
    public static void register(final ClientRegistrar registrar) {
        ClientProxy.set(new ClientProxyImpl());

        registrar.enqueueSetup(BusInterfaceNameRenderer::initialize);
        registrar.enqueueSetup(CustomItemModelProperties::initialize);

        registrar.registerBlockEntityRenderer(BlockEntities.COMPUTER, ComputerRenderer::new);
        registrar.registerBlockEntityRenderer(BlockEntities.MONITOR, MonitorRenderer::new);
        registrar.registerBlockEntityRenderer(BlockEntities.DISK_DRIVE, DiskDriveRenderer::new);
        registrar.registerBlockEntityRenderer(BlockEntities.CHARGER, ChargerRenderer::new);
        registrar.registerBlockEntityRenderer(BlockEntities.PROJECTOR, ProjectorRenderer::new);
        registrar.registerBlockEntityRenderer(
                BlockEntities.INTERNET_GATEWAY, InternetGateWayRenderer::new);

        registrar.registerScreen(Containers.COMPUTER, ComputerContainerScreen::new);
        registrar.registerScreen(Containers.COMPUTER_TERMINAL, ComputerTerminalScreen::new);
        registrar.registerScreen(Containers.MONITOR, MonitorDisplayScreen::new);
        registrar.registerScreen(Containers.ROBOT, RobotContainerScreen::new);
        registrar.registerScreen(Containers.ROBOT_TERMINAL, RobotTerminalScreen::new);
        registrar.registerScreen(Containers.NETWORK_TUNNEL, NetworkTunnelScreen::new);

        registrar.registerEntityRenderer(Entities.ROBOT, RobotRenderer::new);
        registrar.registerLayerDefinition(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);

        registrar.registerBlockColor(new BusCableBlockColor(), List.of(Blocks.BUS_CABLE));
        CustomItemColors.initialize(registrar);

        ClientEvents.HIDE_HOTBAR.register(() -> KeyboardScreen.hideHotbar);
    }
}
