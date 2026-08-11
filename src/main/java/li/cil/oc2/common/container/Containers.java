package li.cil.oc2.common.container;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.screen.computer.ComputerContainerScreen;
import li.cil.oc2.client.gui.screen.computer.ComputerTerminalScreen;
import li.cil.oc2.client.gui.screen.monitor.MonitorDisplayScreen;
import li.cil.oc2.client.gui.screen.network.NetworkTunnelScreen;
import li.cil.oc2.client.gui.screen.robot.RobotContainerScreen;
import li.cil.oc2.client.gui.screen.robot.RobotTerminalScreen;
import li.cil.oc2.common.container.computer.ComputerInventoryContainer;
import li.cil.oc2.common.container.computer.ComputerTerminalContainer;
import li.cil.oc2.common.container.monitor.MonitorDisplayContainer;
import li.cil.oc2.common.container.network.NetworkTunnelContainer;
import li.cil.oc2.common.container.robot.RobotInventoryContainer;
import li.cil.oc2.common.container.robot.RobotTerminalContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = API.MOD_ID)
public final class Containers {
    private static final DeferredRegister<MenuType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.MENU, API.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ComputerInventoryContainer>> COMPUTER =
            REGISTRY.register(
                    "computer",
                    () -> IMenuTypeExtension.create(ComputerInventoryContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<ComputerTerminalContainer>>
            COMPUTER_TERMINAL =
                    REGISTRY.register(
                            "computer_terminal",
                            () ->
                                    IMenuTypeExtension.create(
                                            ComputerTerminalContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<MonitorDisplayContainer>> MONITOR =
            REGISTRY.register(
                    "monitor",
                    () -> IMenuTypeExtension.create(MonitorDisplayContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<RobotInventoryContainer>> ROBOT =
            REGISTRY.register(
                    "robot",
                    () -> IMenuTypeExtension.create(RobotInventoryContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<RobotTerminalContainer>>
            ROBOT_TERMINAL =
                    REGISTRY.register(
                            "robot_terminal",
                            () -> IMenuTypeExtension.create(RobotTerminalContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<NetworkTunnelContainer>>
            NETWORK_TUNNEL =
                    REGISTRY.register(
                            "network_tunnel",
                            () -> IMenuTypeExtension.create(NetworkTunnelContainer::createClient));

    public static void initialize(IEventBus modBus) {
        REGISTRY.register(modBus);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(COMPUTER.get(), ComputerContainerScreen::new);
        event.register(COMPUTER_TERMINAL.get(), ComputerTerminalScreen::new);
        event.register(MONITOR.get(), MonitorDisplayScreen::new);
        event.register(ROBOT.get(), RobotContainerScreen::new);
        event.register(ROBOT_TERMINAL.get(), RobotTerminalScreen::new);
        event.register(NETWORK_TUNNEL.get(), NetworkTunnelScreen::new);
    }
}