package li.cil.oc2.common.container;

import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.common.container.computer.ComputerInventoryContainer;
import li.cil.oc2.common.container.computer.ComputerTerminalContainer;
import li.cil.oc2.common.container.monitor.MonitorDisplayContainer;
import li.cil.oc2.common.container.network.NetworkTunnelContainer;
import li.cil.oc2.common.container.robot.RobotInventoryContainer;
import li.cil.oc2.common.container.robot.RobotTerminalContainer;
import li.cil.oc2.platform.Platform;
import net.minecraft.world.inventory.MenuType;

public final class Containers {
    public static final Supplier<MenuType<ComputerInventoryContainer>> COMPUTER =
            Platform.registries().register("minecraft:menu", API.MOD_ID, 
                    "computer",
                    () -> Platform.menus().createMenuType(ComputerInventoryContainer::createClient));
    public static final Supplier<MenuType<ComputerTerminalContainer>>
            COMPUTER_TERMINAL =
                    Platform.registries().register("minecraft:menu", API.MOD_ID, 
                            "computer_terminal",
                            () ->
                                    Platform.menus().createMenuType(
                                            ComputerTerminalContainer::createClient));
    public static final Supplier<MenuType<MonitorDisplayContainer>> MONITOR =
            Platform.registries().register("minecraft:menu", API.MOD_ID, 
                    "monitor",
                    () -> Platform.menus().createMenuType(MonitorDisplayContainer::createClient));
    public static final Supplier<MenuType<RobotInventoryContainer>> ROBOT =
            Platform.registries().register("minecraft:menu", API.MOD_ID, 
                    "robot",
                    () -> Platform.menus().createMenuType(RobotInventoryContainer::createClient));
    public static final Supplier<MenuType<RobotTerminalContainer>>
            ROBOT_TERMINAL =
                    Platform.registries().register("minecraft:menu", API.MOD_ID, 
                            "robot_terminal",
                            () -> Platform.menus().createMenuType(RobotTerminalContainer::createClient));
    public static final Supplier<MenuType<NetworkTunnelContainer>>
            NETWORK_TUNNEL =
                    Platform.registries().register("minecraft:menu", API.MOD_ID, 
                            "network_tunnel",
                            () -> Platform.menus().createMenuType(NetworkTunnelContainer::createClient));

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }
}
