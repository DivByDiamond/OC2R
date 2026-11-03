package li.cil.oc2.common.integration.create;

import java.util.Optional;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Adds controls to the adjacent block reached by an ordinary bus interface. */
public final class CreateControlsProvider extends AbstractBlockEntityDeviceProvider<BlockEntity> {
    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final BlockEntity blockEntity) {
        return CreateControlsDevice.find(blockEntity).map(ObjectDevice::new);
    }
}
