package li.cil.oc2.client.gui;

import java.util.function.Supplier;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ScreenRegistry {
    private ScreenRegistry() {}

    public static <T extends AbstractContainerMenu, U extends Screen & MenuAccess<T>> void register(
            final RegisterMenuScreensEvent event,
            final Supplier<? extends MenuType<T>> type,
            final MenuScreens.ScreenConstructor<T, U> factory) {
        event.register(type.get(), factory);
    }
}
