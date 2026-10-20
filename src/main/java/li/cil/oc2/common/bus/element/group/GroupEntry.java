package li.cil.oc2.common.bus.element.group;

import java.util.Optional;
import li.cil.oc2.api.bus.device.Device;

public interface GroupEntry {
    Optional<String> getDeviceDataKey();

    /** Stable member identity when a provider publishes a group of devices. */
    default Optional<String> getDeviceDataSubKey() {
        return Optional.empty();
    }

    Optional<String> getLegacyDeviceDataKey();

    int getDeviceEnergyConsumption();

    Device getDevice();
}
