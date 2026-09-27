package li.cil.oc2.common.container;

import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.ScreenRegistry;
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
import li.cil.oc2.platform.NeoForgeRegistryBridge;
import li.cil.oc2.platform.Platform;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

@EventBusSubscriber(modid = API.MOD_ID)
public final class Containers {
    public static final Supplier<MenuType<ComputerInventoryContainer>> COMPUTER =
            Platform.registries().register("minecraft:menu", API.MOD_ID, 
                    "computer",
                    () -> IMenuTypeExtension.create(ComputerInventoryContainer::createClient));
    public static final Supplier<MenuType<ComputerTerminalContainer>>
            COMPUTER_TERMINAL =
                    Platform.registries().register("minecraft:menu", API.MOD_ID, 
                            "computer_terminal",
                            () ->
                                    IMenuTypeExtension.create(
                                            ComputerTerminalContainer::createClient));
    public static final Supplier<MenuType<MonitorDisplayContainer>> MONITOR =
            Platform.registries().register("minecraft:menu", API.MOD_ID, 
                    "monitor",
                    () -> IMenuTypeExtension.create(MonitorDisplayContainer::createClient));
    public static final Supplier<MenuType<RobotInventoryContainer>> ROBOT =
            Platform.registries().register("minecraft:menu", API.MOD_ID, 
                    "robot",
                    () -> IMenuTypeExtension.create(RobotInventoryContainer::createClient));
    public static final Supplier<MenuType<RobotTerminalContainer>>
            ROBOT_TERMINAL =
                    Platform.registries().register("minecraft:menu", API.MOD_ID, 
                            "robot_terminal",
                            () -> IMenuTypeExtension.create(RobotTerminalContainer::createClient));
    public static final Supplier<MenuType<NetworkTunnelContainer>>
            NETWORK_TUNNEL =
                    Platform.registries().register("minecraft:menu", API.MOD_ID, 
                            "network_tunnel",
                            () -> IMenuTypeExtension.create(NetworkTunnelContainer::createClient));

    public static void initialize(IEventBus modBus) {
        NeoForgeRegistryBridge.instance().bind(modBus);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        ScreenRegistry.register(event, COMPUTER, ComputerContainerScreen::new);
        ScreenRegistry.register(event, COMPUTER_TERMINAL, ComputerTerminalScreen::new);
        ScreenRegistry.register(event, MONITOR, MonitorDisplayScreen::new);
        ScreenRegistry.register(event, ROBOT, RobotContainerScreen::new);
        ScreenRegistry.register(event, ROBOT_TERMINAL, RobotTerminalScreen::new);
        ScreenRegistry.register(event, NETWORK_TUNNEL, NetworkTunnelScreen::new);
    }
}