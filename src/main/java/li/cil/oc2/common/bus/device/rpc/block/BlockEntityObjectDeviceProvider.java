package li.cil.oc2.common.bus.device.rpc.block;

import java.util.Optional;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.SidedBlockDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockEntityObjectDeviceProvider
        extends AbstractBlockEntityDeviceProvider<BlockEntity> {
    @Override
    public Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final BlockEntity blockEntity) {
        if (!SidedBlockDevice.permits(blockEntity.getBlockState(), query.getQuerySide())) return Optional.empty();
        if (Callbacks.hasMethods(blockEntity)) {
            return Optional.of(new ObjectDevice(blockEntity));
        } else {
            return Optional.empty();
        }
    }
}