package li.cil.oc2.common.bus.device.util.info;

import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;

public final class BlockDeviceInfo extends AbstractDeviceInfo<BlockDeviceProvider, Device> {
    public BlockDeviceInfo(
            @Nullable final BlockDeviceProvider blockDeviceProvider, final Device device) {
        super(blockDeviceProvider, device);
    }
}