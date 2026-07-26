/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.rpc.FluidHandlerDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Optional;

public final class FluidHandlerItemDeviceProvider extends AbstractItemStackCapabilityDeviceProvider<IFluidHandlerItem> {
    public FluidHandlerItemDeviceProvider() {
        super(() -> Capabilities.FluidHandler.ITEM);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query, final IFluidHandlerItem value) {
        return Optional.of(new ObjectDevice(new FluidHandlerDevice(value)));
    }
}
