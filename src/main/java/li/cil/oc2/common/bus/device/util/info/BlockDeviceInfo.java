package li.cil.oc2.common.bus.device.util.info;

import javax.annotation.Nullable;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;

public final class BlockDeviceInfo extends AbstractDeviceInfo<BlockDeviceProvider, Device> {
    @Nullable public final String groupKey;

    public BlockDeviceInfo(
            @Nullable final BlockDeviceProvider blockDeviceProvider, final Device device) {
        this(blockDeviceProvider, device, null);
    }

    public BlockDeviceInfo(
            @Nullable final BlockDeviceProvider blockDeviceProvider,
            final Device device,
            @Nullable final String groupKey) {
        super(blockDeviceProvider, device);
        this.groupKey = groupKey;
    }
}
