package li.cil.oc2.common.vm.handler;

import java.util.Optional;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.platform.ItemHandler;

public interface VMItemStackHandlers {
    Optional<ItemHandler> getItemHandler(DeviceType deviceType);

    boolean isEmpty();

    void exportDeviceDataToItemStacks();
}