package li.cil.oc2.common.bus.element;

import java.util.Optional;
import li.cil.oc2.api.bus.device.Device;

interface GroupEntry {
    Optional<String> getDeviceDataKey();

    Optional<String> getLegacyDeviceDataKey();

    int getDeviceEnergyConsumption();

    Device getDevice();
}