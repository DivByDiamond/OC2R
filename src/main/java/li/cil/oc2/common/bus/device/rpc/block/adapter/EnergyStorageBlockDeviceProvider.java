package li.cil.oc2.common.bus.device.rpc.block.adapter;

import java.util.Optional;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.adapter.EnergyStorageDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.EnergyStorage;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class EnergyStorageBlockDeviceProvider
        extends AbstractBlockEntityCapabilityDeviceProvider<EnergyStorage, BlockEntity> {
    public EnergyStorageBlockDeviceProvider() {
        super(() -> Capabilities.EnergyStorage.BLOCK);
    }

    @Override
    protected Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final EnergyStorage value) {
        return Optional.of(new ObjectDevice(new EnergyStorageDevice(value)));
    }
}