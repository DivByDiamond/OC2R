package li.cil.oc2.common.vm.memory;

import java.util.OptionalLong;
import li.cil.oc2.api.bus.device.vm.VMDevice;

public interface BaseAddressProvider {
    OptionalLong getBaseAddress(final VMDevice wrapper);
}