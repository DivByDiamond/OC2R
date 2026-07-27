package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.components.RestrictedContainer;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public abstract class AbstractTypedDeviceItemStackHandler extends AbstractDeviceItemStackHandler {
    private final DeviceType deviceType;

    public AbstractTypedDeviceItemStackHandler(
            Supplier<HolderLookup.Provider> providerSupplier,
            final int size,
            final DeviceType deviceType) {
        super(providerSupplier, size);
        this.deviceType = deviceType;
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return super.isItemValid(slot, stack) && stack.is(deviceType.getTag());
    }

    public void loadItems(final HolderLookup.Provider registries, RestrictedContainer container) {
        var containerOfType =
                container
                        .items()
                        .getOrDefault(this.deviceType.getTag(), NonNullList.of(ItemStack.EMPTY));
        for (int slot = 0; slot < getSlots() && slot < containerOfType.size(); slot++) {
            setStackInSlot(slot, containerOfType.get(slot));
            getBusElement().handleSlotContentsChanged(registries, slot, getStackInSlot(slot));
        }
    }

    public void saveItems(RestrictedContainer container) {
        container.items().put(this.deviceType.getTag(), NonNullList.copyOf(this.stacks));
    }
}
