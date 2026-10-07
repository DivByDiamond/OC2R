package li.cil.oc2.platform;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Loader-independent creation and opening of menus that carry extra data from the server to the
 * client. A stateless singleton discovered through {@link java.util.ServiceLoader} via
 * {@link Platform#menus()}.
 */
public interface MenuBridge {
    /** Creates a menu type whose client side is built from {@code factory} and the server's extra data. */
    <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuFactory<T> factory);

    /**
     * Opens the menu {@code provider} creates for {@code player}, sending whatever {@code extraData}
     * writes to the client-side {@link MenuFactory}.
     */
    void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData);

    /** Opens the menu with the block position it belongs to as the extra data. */
    default void openMenu(final ServerPlayer player, final MenuProvider provider, final BlockPos pos) {
        openMenu(player, provider, buffer -> buffer.writeBlockPos(pos));
    }

    /** Builds the client side of a menu from the data the server wrote when opening it. */
    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int id, Inventory inventory, FriendlyByteBuf data);
    }
}
