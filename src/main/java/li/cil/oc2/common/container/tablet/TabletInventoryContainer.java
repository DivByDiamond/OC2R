package li.cil.oc2.common.container.tablet;

import java.util.UUID;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.container.slot.DeviceTypeSlotItemHandler;
import li.cil.oc2.common.tablet.TabletInventory;
import li.cil.oc2.common.tablet.TabletManager;
import li.cil.oc2.platform.ItemHandler;
import li.cil.oc2.platform.Platform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** The two parts a player may swap: processor and flash. Memory is fixed and has no slot. */
public final class TabletInventoryContainer extends AbstractTabletContainer {
    public static final int CPU_X = 62;
    public static final int FLASH_X = 98;
    public static final int SLOTS_Y = 24;
    public static final int PLAYER_INVENTORY_Y = 62;

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
                        return new TabletInventoryContainer(id, owner, tabletId);
                    }
                },
                buffer -> buffer.writeUUID(tabletId));
    }

    public static TabletInventoryContainer createClient(
            final int id, final Inventory inventory, final FriendlyByteBuf data) {
        return new TabletInventoryContainer(id, inventory.player, data.readUUID());
    }

    private TabletInventoryContainer(final int id, final Player player, final UUID tabletId) {
        super(Containers.TABLET.get(), id, player, tabletId);
        if (player instanceof final ServerPlayer serverPlayer) {
            final var machine = TabletManager.machineOf(serverPlayer.level().getServer(), tabletId);
            if (machine != null) addDeviceSlots(machine.inventory());
        } else {
            // The client mirrors the slot layout; contents are synchronized by the menu.
            addDeviceSlots(new TabletInventory(() -> player.level().registryAccess(), true));
        }
        createPlayerInventoryAndHotbarSlots(player.getInventory(), 8, PLAYER_INVENTORY_Y);
    }

    private void addDeviceSlots(final TabletInventory inventory) {
        inventory
                .getItemHandler(DeviceTypes.CPU)
                .ifPresent(handler -> addSlot(slot(handler, DeviceTypes.CPU, CPU_X)));
        inventory
                .getItemHandler(DeviceTypes.FLASH_MEMORY)
                .ifPresent(handler -> addSlot(slot(handler, DeviceTypes.FLASH_MEMORY, FLASH_X)));
    }

    private static Slot slot(final ItemHandler handler, final li.cil.oc2.api.bus.device.DeviceType type, final int x) {
        return new DeviceTypeSlotItemHandler(handler, type, 0, x, SLOTS_Y);
    }
}
