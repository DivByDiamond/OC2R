package li.cil.oc2.common.bus.element;

import li.cil.oc2.api.bus.device.Device;

import java.util.Optional;

interface GroupEntry {
    Optional<String> getDeviceDataKey();

    Optional<String> getLegacyDeviceDataKey();

    int getDeviceEnergyConsumption();

    Device getDevice();
}
