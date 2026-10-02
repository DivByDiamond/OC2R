package li.cil.oc2.common.bus.device.rpc.adapter;

import java.util.Collection;
import java.util.Collections;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.platform.ItemHandler;
import net.minecraft.world.item.ItemStack;

public final class ItemHandlerDevice extends IdentityProxy<ItemHandler> implements NamedDevice {
    public ItemHandlerDevice(final ItemHandler identity) {
        super(identity);
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singleton("item_handler");
    }

    @Callback
    public int getItemSlotCount() {
        return identity.getSlots();
    }

    @Callback
    public ItemStack getItemStackInSlot(final int slot) {
        return identity.getStackInSlot(slot);
    }

    @Callback
    public int getItemSlotLimit(final int slot) {
        return identity.getSlotLimit(slot);
    }
}