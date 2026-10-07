package li.cil.oc2.common.bus.device.provider.item;

import java.util.Optional;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.rpc.adapter.ItemHandlerDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.ItemHandler;

public final class ItemHandlerItemDeviceProvider
        extends AbstractItemStackCapabilityDeviceProvider<ItemHandler> {
    public ItemHandlerItemDeviceProvider() {
        super(() -> Capabilities.ItemHandler.ITEM);
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(
            final ItemDeviceQuery query, final ItemHandler value) {
        return Optional.of(new ObjectDevice(new ItemHandlerDevice(value)));
    }
}