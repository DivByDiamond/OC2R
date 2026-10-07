package li.cil.oc2.common.bus.device.rpc.block.adapter;

import java.util.Optional;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.adapter.ItemHandlerDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.ItemHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ItemHandlerBlockDeviceProvider
        extends AbstractBlockEntityCapabilityDeviceProvider<ItemHandler, BlockEntity> {
    public ItemHandlerBlockDeviceProvider() {
        super(() -> Capabilities.ItemHandler.BLOCK);
    }

    @Override
    protected Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final ItemHandler value) {
        return Optional.of(new ObjectDevice(new ItemHandlerDevice(value)));
    }
}