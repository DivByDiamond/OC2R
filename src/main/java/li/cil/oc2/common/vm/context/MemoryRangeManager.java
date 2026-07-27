package li.cil.oc2.common.vm.context;

import java.util.OptionalLong;
import li.cil.sedna.api.device.MemoryMappedDevice;

public interface MemoryRangeManager {
    OptionalLong findMemoryRange(MemoryMappedDevice device, long start);

    void releaseMemoryRange(MemoryMappedDevice device);
}