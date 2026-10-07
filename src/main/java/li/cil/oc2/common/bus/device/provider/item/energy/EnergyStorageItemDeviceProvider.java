package li.cil.oc2.common.bus.device.provider.item.energy;

import java.util.Optional;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.item.AbstractItemStackCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.adapter.EnergyStorageDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.platform.EnergyStorage;

public final class EnergyStorageItemDeviceProvider
        extends AbstractItemStackCapabilityDeviceProvider<EnergyStorage> {
    public EnergyStorageItemDeviceProvider() {
        super(() -> Capabilities.EnergyStorage.ITEM);
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(
            final ItemDeviceQuery query, final EnergyStorage value) {
        return Optional.of(new ObjectDevice(new EnergyStorageDevice(value)));
    }
}