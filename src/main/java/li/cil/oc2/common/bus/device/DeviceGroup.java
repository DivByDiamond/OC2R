package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class DeviceGroup extends IdentityProxy<BlockEntity> implements Device {
    private final Set<Device> devices = new HashSet<>();

    public DeviceGroup(final BlockEntity identity) {
        super(identity);
    }

    public void addDevice(Device device)
    {
        devices.add(device);
    }

    public Set<Device> getDevices() {
        return Collections.unmodifiableSet(devices);
    }
}
