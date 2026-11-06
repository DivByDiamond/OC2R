package li.cil.oc2.common.container.tablet;

import java.util.UUID;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.platform.Platform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** The big screen of a tablet: the guest terminal with keyboard capture. */
public final class TabletTerminalContainer extends AbstractTabletContainer {
    public static void open(final ServerPlayer player, final UUID tabletId) {
        Platform.menus().openMenu(
                player,
                new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("item.oc2r.tablet");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(
                            final int id, final Inventory inventory, final Player owner) {
                        return new TabletTerminalContainer(id, owner, tabletId);
                    }
                },
                buffer -> buffer.writeUUID(tabletId));
    }

    public static TabletTerminalContainer createClient(
            final int id, final Inventory inventory, final FriendlyByteBuf data) {
        return new TabletTerminalContainer(id, inventory.player, data.readUUID());
    }

    private TabletTerminalContainer(final int id, final Player player, final UUID tabletId) {
        super(Containers.TABLET_TERMINAL.get(), id, player, tabletId);
    }
}
