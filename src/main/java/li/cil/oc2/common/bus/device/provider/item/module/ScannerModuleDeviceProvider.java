package li.cil.oc2.common.bus.device.provider.item.module;

import java.util.Optional;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.item.module.ScannerModuleDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.platform.Platform;

public final class ScannerModuleDeviceProvider extends AbstractItemDeviceProvider {
    public ScannerModuleDeviceProvider() {
        super(Items.SCANNER_MODULE);
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return query.getContainerEntity()
                .flatMap(
                        entity ->
                                Optional.ofNullable(
                                                Platform.capabilities()
                                                        .getEntityCapability(
                                                                Capabilities.Robot.ENTITY,
                                                                entity,
                                                                null))
                                        .map(
                                                robot ->
                                                        new ScannerModuleDevice(
                                                                query.getItemStack(), entity)));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Config.scannerModuleEnergyPerTick;
    }
}
