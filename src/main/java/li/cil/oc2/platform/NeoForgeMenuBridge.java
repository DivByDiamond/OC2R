package li.cil.oc2.platform;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

/** {@link MenuBridge} backed by NeoForge's menu type extension. */
public final class NeoForgeMenuBridge implements MenuBridge {
    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(final MenuFactory<T> factory) {
        return IMenuTypeExtension.create(factory::create);
    }

    @Override
    public void openMenu(
            final ServerPlayer player, final MenuProvider provider, final Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(provider, extraData::accept);
    }
}
