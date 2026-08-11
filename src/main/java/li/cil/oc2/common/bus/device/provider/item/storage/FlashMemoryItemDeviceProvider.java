package li.cil.oc2.common.bus.device.provider.item.storage;

import java.util.Optional;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.storage.ByteBufferFlashStorageDevice;
import li.cil.oc2.common.item.storage.flash.FlashMemoryItem;
import net.minecraft.world.item.ItemStack;

public final class FlashMemoryItemDeviceProvider extends AbstractItemDeviceProvider {
    public FlashMemoryItemDeviceProvider() {
        super(FlashMemoryItem.class);
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final FlashMemoryItem item = (FlashMemoryItem) stack.getItem();

        final int capacity = Math.max(item.getCapacity(stack), 0);
        return Optional.of(new ByteBufferFlashStorageDevice(stack, capacity));
    }
}