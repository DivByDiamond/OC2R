package li.cil.oc2.common.bus.device;

import java.util.*;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DeviceGroup extends IdentityProxy<BlockEntity> implements Device {
    // Group membership is configured on the server thread before bus discovery.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<Device, String> devices = new HashMap<>();

    public DeviceGroup(final BlockEntity identity) {
        super(identity);
    }

    public void addDevice(Device device) {
        addDevice(device.getClass().getName(), device);
    }

    public void addDevice(final String key, final Device device) {
        if (key.equals(devices.get(device))) return;
        if (key.isEmpty() || devices.containsValue(key)) {
            throw new IllegalArgumentException("Device group member keys must be unique and nonempty");
        }
        if (devices.containsKey(device)) {
            throw new IllegalArgumentException("Device is already a member of this group");
        }
        devices.put(device, key);
    }

    public String getDeviceKey(final Device device) {
        return Objects.requireNonNull(devices.get(device));
    }

    public Set<Device> getDevices() {
        return Collections.unmodifiableSet(devices.keySet());
    }
}
