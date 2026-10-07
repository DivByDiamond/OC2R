package li.cil.oc2.platform;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * {@link MenuBridge} for Fabric. Fabric's extended menus carry a typed payload, so the extra data
 * the mod writes into a buffer travels as a plain byte array and is unwrapped again on the client.
 */
public final class FabricMenuBridge implements MenuBridge {
    private static final StreamCodec<ByteBuf, byte[]> BYTES = ByteBufCodecs.BYTE_ARRAY;

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(final MenuFactory<T> factory) {
        return new ExtendedScreenHandlerType<>(
                (id, inventory, data) -> factory.create(id, inventory, wrap(data, inventory.player)), BYTES);
    }

    @Override
    public void openMenu(
            final ServerPlayer player, final MenuProvider provider, final Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(new ExtendedScreenHandlerFactory<byte[]>() {
            @Override
            public byte[] getScreenOpeningData(final ServerPlayer opener) {
                final RegistryFriendlyByteBuf buffer =
                        new RegistryFriendlyByteBuf(Unpooled.buffer(), opener.registryAccess());
                extraData.accept(buffer);
                final byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                return bytes;
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player opener) {
                return provider.createMenu(id, inventory, opener);
            }
        });
    }

    private static FriendlyByteBuf wrap(final byte[] data, final Player player) {
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), player.registryAccess());
    }
}
