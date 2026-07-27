package li.cil.oc2.common.container;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.screen.*;

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
    private static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(BuiltInRegistries.MENU, API.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ComputerInventoryContainer>> COMPUTER =
            CONTAINERS.register(
                    "computer",
                    () -> IMenuTypeExtension.create(ComputerInventoryContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<ComputerTerminalContainer>>
            COMPUTER_TERMINAL =
                    CONTAINERS.register(
                            "computer_terminal",
                            () ->
                                    IMenuTypeExtension.create(
                                            ComputerTerminalContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<MonitorDisplayContainer>> MONITOR =
            CONTAINERS.register(
                    "monitor",
                    () -> IMenuTypeExtension.create(MonitorDisplayContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<RobotInventoryContainer>> ROBOT =
            CONTAINERS.register(
                    "robot",
                    () -> IMenuTypeExtension.create(RobotInventoryContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<RobotTerminalContainer>>
            ROBOT_TERMINAL =
                    CONTAINERS.register(
                            "robot_terminal",
                            () -> IMenuTypeExtension.create(RobotTerminalContainer::createClient));
    public static final DeferredHolder<MenuType<?>, MenuType<NetworkTunnelContainer>>
            NETWORK_TUNNEL =
                    CONTAINERS.register(
                            "network_tunnel",
                            () -> IMenuTypeExtension.create(NetworkTunnelContainer::createClient));

    public static void initialize(IEventBus modBus) {
        CONTAINERS.register(modBus);
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
