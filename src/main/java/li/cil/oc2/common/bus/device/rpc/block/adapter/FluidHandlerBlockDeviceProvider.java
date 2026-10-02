package li.cil.oc2.common.bus.device.rpc.block.adapter;

import java.util.Optional;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.adapter.FluidHandlerDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.FluidHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FluidHandlerBlockDeviceProvider
        extends AbstractBlockEntityCapabilityDeviceProvider<FluidHandler, BlockEntity> {
    public FluidHandlerBlockDeviceProvider() {
        super(() -> Capabilities.FluidHandler.BLOCK);
    }

    @Override
    protected Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final FluidHandler value) {
        return Optional.of(new ObjectDevice(new FluidHandlerDevice(value)));
    }
}