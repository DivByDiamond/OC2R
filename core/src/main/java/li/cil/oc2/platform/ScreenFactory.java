package li.cil.oc2.platform;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Builds the screen for a menu; the loader-independent twin of vanilla's non-public screen constructor. */
@FunctionalInterface
public interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
    S create(M menu, Inventory inventory, Component title);
}
