package li.cil.oc2.common.bus.device.rpc.adapter;

import java.util.Collection;
import java.util.Collections;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.platform.FluidHandler;
import li.cil.oc2.platform.FluidStack;

public final class FluidHandlerDevice extends IdentityProxy<FluidHandler> implements NamedDevice {
    public FluidHandlerDevice(final FluidHandler identity) {
        super(identity);
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singletonList("fluid_handler");
    }

    @Callback
    public int getFluidTanks() {
        return identity.getTanks();
    }

    @Callback
    public FluidStack getFluidInTank(final int tank) {
        return identity.getFluidInTank(tank);
    }

    @Callback
    public int getFluidTankCapacity(final int tank) {
        return identity.getTankCapacity(tank);
    }
}