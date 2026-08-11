package li.cil.oc2.common.bus.device.provider.item.energy;

import java.util.Optional;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.item.AbstractItemStackCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.adapter.FluidHandlerDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public final class FluidHandlerItemDeviceProvider
        extends AbstractItemStackCapabilityDeviceProvider<IFluidHandlerItem> {
    public FluidHandlerItemDeviceProvider() {
        super(() -> Capabilities.FluidHandler.ITEM);
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(
            final ItemDeviceQuery query, final IFluidHandlerItem value) {
        return Optional.of(new ObjectDevice(new FluidHandlerDevice(value)));
    }
}